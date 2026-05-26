package com.hospital.medapp.utils;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PPGProcessor – Fotopletismografía (PPG) usando la cámara trasera + flash LED.
 *
 * PRINCIPIO FÍSICO:
 *  • Frecuencia cardíaca: la hemoglobina absorbe la luz verde (~530 nm) con
 *    alta eficiencia. Cada latido aumenta el flujo sanguíneo subcutáneo,
 *    variando la cantidad de luz verde reflejada. Se analiza el canal VERDE
 *    del sensor de imagen para detectar los picos (pulsaciones).
 *
 *  • SpO₂: la hemoglobina oxigenada (HbO₂) y la desoxigenada (Hb) absorben
 *    de forma diferente en rojo (~660 nm) e infrarrojo (~880 nm). La mayoría
 *    de teléfonos solo tiene luz roja (flash LED ≈ 600–700 nm) y sensor RGB,
 *    por lo que se usa la relación canal ROJO / canal AZUL como aproximación.
 *    NOTA: esta estimación es orientativa; para medición clínica se requiere
 *    un oxímetro de pulso certificado con LED IR.
 *
 * USO:
 *   ppgProcessor = new PPGProcessor(context);
 *   ppgProcessor.startMeasurement(new PPGProcessor.PPGListener() { ... });
 *   // El usuario debe colocar el dedo sobre la cámara trasera
 *   // Después de ~30 s:
 *   ppgProcessor.stopMeasurement();
 */
public class PPGProcessor {

    public interface PPGListener {
        void onHeartRateUpdated(double bpm);
        void onSpO2Updated(double spo2);
        void onMeasurementComplete(double bpm, double spo2);
        void onError(String message);
    }

    private static final String TAG              = "PPGProcessor";
    private static final int    SAMPLE_DURATION_MS = 30_000;  // 30 segundos
    private static final int    MIN_PEAKS_NEEDED   = 5;
    private static final double MIN_AMPLITUDE_RATIO = 0.05;   // umbral ruido

    // Estado del sensor
    private final android.content.Context context;
    private CameraManager    cameraManager;
    private CameraDevice     cameraDevice;
    private ImageReader      imageReader;
    private HandlerThread    backgroundThread;
    private Handler          backgroundHandler;
    private CameraCaptureSession captureSession;
    private PPGListener      listener;

    // Buffers de señal
    private final List<Double> redSamples   = new ArrayList<>();
    private final List<Double> greenSamples = new ArrayList<>();
    private final List<Double> blueSamples  = new ArrayList<>();
    private final List<Long>   timestamps   = new ArrayList<>();

    private boolean measuring = false;
    private long    startTime;

    public PPGProcessor(android.content.Context context) {
        this.context      = context.getApplicationContext();
        this.cameraManager = (CameraManager)
                context.getSystemService(android.content.Context.CAMERA_SERVICE);
    }

    // ─── API pública ──────────────────────────────────────────────────────────

    public void startMeasurement(PPGListener listener) {
        this.listener = listener;
        redSamples.clear();
        greenSamples.clear();
        blueSamples.clear();
        timestamps.clear();
        measuring  = true;
        startTime  = System.currentTimeMillis();
        startBackgroundThread();
        openCamera();
    }

    public void stopMeasurement() {
        measuring = false;
        closeCamera();
        stopBackgroundThread();
    }

    // ─── Cámara ───────────────────────────────────────────────────────────────

    private void openCamera() {
        try {
            String cameraId = getRearCameraId();
            if (cameraId == null) {
                if (listener != null) listener.onError("No se encontró cámara trasera.");
                return;
            }
            // 320 × 240 a baja resolución para máxima velocidad de muestreo
            imageReader = ImageReader.newInstance(320, 240,
                    ImageFormat.YUV_420_888, 4);
            imageReader.setOnImageAvailableListener(
                    imageAvailableListener, backgroundHandler);

            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);

        } catch (CameraAccessException | SecurityException e) {
            Log.e(TAG, "Error al abrir cámara: " + e.getMessage());
            if (listener != null) listener.onError("Permiso de cámara denegado.");
        }
    }

    private String getRearCameraId() throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            CameraCharacteristics ch = cameraManager.getCameraCharacteristics(id);
            Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK)
                return id;
        }
        return null;
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCaptureSession();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) { camera.close(); }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            if (listener != null) listener.onError("Error de cámara: " + error);
        }
    };

    private void createCaptureSession() {
        try {
            List<android.view.Surface> surfaces =
                    Collections.singletonList(imageReader.getSurface());

            cameraDevice.createCaptureSession(surfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            startCapture();
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            if (listener != null) listener.onError("Error configurando cámara.");
                        }
                    }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void startCapture() {
        try {
            CaptureRequest.Builder builder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(imageReader.getSurface());
            // Activar flash para iluminar el dedo
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            // Auto-exposición desactivada para estabilidad de señal
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8_000_000L); // 8 ms
            captureSession.setRepeatingRequest(
                    builder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void closeCamera() {
        try {
            if (captureSession != null) { captureSession.close(); captureSession = null; }
            if (cameraDevice  != null) { cameraDevice.close();   cameraDevice   = null; }
            if (imageReader   != null) { imageReader.close();    imageReader    = null; }
        } catch (Exception e) {
            Log.e(TAG, "Error cerrando cámara: " + e.getMessage());
        }
    }

    // ─── Procesamiento de imagen ──────────────────────────────────────────────

    private final ImageReader.OnImageAvailableListener imageAvailableListener =
            reader -> {
                try (Image image = reader.acquireLatestImage()) {
                    if (image == null || !measuring) return;

                    long now = System.currentTimeMillis();
                    long elapsed = now - startTime;

                    // Extraer planos YUV → convertir a RGB promedio de la región central
                    double[] rgb = extractAverageRGB(image);
                    double red   = rgb[0];
                    double green = rgb[1];
                    double blue  = rgb[2];

                    redSamples  .add(red);
                    greenSamples.add(green);
                    blueSamples .add(blue);
                    timestamps  .add(now);

                    // Estimación parcial cada 5 s (feedback en tiempo real)
                    if (elapsed > 5000 && greenSamples.size() % 30 == 0) {
                        double partialBpm  = estimateBPM(greenSamples, timestamps);
                        double partialSpo2 = estimateSpO2(redSamples, blueSamples);
                        if (listener != null) {
                            new Handler(android.os.Looper.getMainLooper()).post(() -> {
                                listener.onHeartRateUpdated(partialBpm);
                                listener.onSpO2Updated(partialSpo2);
                            });
                        }
                    }

                    // Medición completa
                    if (elapsed >= SAMPLE_DURATION_MS) {
                        measuring = false;
                        double finalBpm  = estimateBPM(greenSamples, timestamps);
                        double finalSpo2 = estimateSpO2(redSamples, blueSamples);
                        closeCamera();
                        stopBackgroundThread();
                        if (listener != null) {
                            new Handler(android.os.Looper.getMainLooper()).post(() ->
                                    listener.onMeasurementComplete(finalBpm, finalSpo2));
                        }
                    }
                }
            };

    /**
     * Convierte un frame YUV_420_888 a canales RGB promediados sobre
     * la región central (40 % del área) donde se apoya el dedo.
     */
    private double[] extractAverageRGB(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuf  = planes[0].getBuffer();
        ByteBuffer uBuf  = planes[1].getBuffer();
        ByteBuffer vBuf  = planes[2].getBuffer();

        int width  = image.getWidth();
        int height = image.getHeight();
        int xStart = width  / 4;
        int xEnd   = width  * 3 / 4;
        int yStart = height / 4;
        int yEnd   = height * 3 / 4;

        long sumR = 0, sumG = 0, sumB = 0;
        int count = 0;

        for (int row = yStart; row < yEnd; row++) {
            for (int col = xStart; col < xEnd; col++) {
                int Y  = (yBuf.get(row * planes[0].getRowStride() + col) & 0xFF);
                int uvRow  = row / 2;
                int uvCol  = col / 2;
                int uvIdx  = uvRow * planes[1].getRowStride()
                           + uvCol * planes[1].getPixelStride();
                int Cb = (uBuf.get(uvIdx) & 0xFF) - 128;
                int Cr = (vBuf.get(uvIdx) & 0xFF) - 128;

                // Conversión YCbCr → RGB
                int R = clamp((int)(Y + 1.402 * Cr));
                int G = clamp((int)(Y - 0.344136 * Cb - 0.714136 * Cr));
                int B = clamp((int)(Y + 1.772 * Cb));

                sumR += R;
                sumG += G;
                sumB += B;
                count++;
            }
        }

        if (count == 0) return new double[]{0, 0, 0};
        return new double[]{sumR / (double) count,
                            sumG / (double) count,
                            sumB / (double) count};
    }

    private int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // ─── Algoritmo de estimación BPM (detección de picos en señal verde) ──────

    /**
     * Estima BPM usando detección de picos en la señal verde normalizada.
     *
     * Pasos:
     *  1. Normalizar señal (z-score).
     *  2. Suavizar con media móvil (filtro paso-bajo sencillo).
     *  3. Detectar cruces ascendentes por cero → picos sistólicos.
     *  4. Calcular RR-intervals → BPM = 60 / (RR_promedio_en_segundos).
     */
    private double estimateBPM(List<Double> signal, List<Long> ts) {
        if (signal.size() < 30) return 0;

        List<Double> normalized = zNormalize(signal);
        List<Double> smoothed   = movingAverage(normalized, 5);

        // Detectar picos locales
        List<Integer> peakIndices = new ArrayList<>();
        for (int i = 2; i < smoothed.size() - 2; i++) {
            double curr = smoothed.get(i);
            if (curr > 0.4                              // por encima de la media
                    && curr > smoothed.get(i - 1)
                    && curr > smoothed.get(i - 2)
                    && curr > smoothed.get(i + 1)
                    && curr > smoothed.get(i + 2)) {
                // Evitar picos demasiado cercanos (< 300 ms → > 200 BPM, fisiológicamente imposible)
                if (!peakIndices.isEmpty()) {
                    long dt = ts.get(i) - ts.get(peakIndices.get(peakIndices.size() - 1));
                    if (dt < 300) continue;
                }
                peakIndices.add(i);
            }
        }

        if (peakIndices.size() < MIN_PEAKS_NEEDED) return 0;

        // Calcular BPM promedio a partir de los intervalos RR
        List<Double> rrMs = new ArrayList<>();
        for (int k = 1; k < peakIndices.size(); k++) {
            long dt = ts.get(peakIndices.get(k)) - ts.get(peakIndices.get(k - 1));
            rrMs.add((double) dt);
        }
        double avgRR = average(rrMs);
        return (avgRR > 0) ? 60_000.0 / avgRR : 0;
    }

    // ─── Estimación SpO₂ (relación R/IR aproximada con canal rojo/azul) ──────

    /**
     * Calcula SpO₂ usando la relación de perfusión (RoR):
     *   RoR = (AC_red / DC_red) / (AC_blue / DC_blue)
     *
     * La curva de calibración empírica para SpO₂:
     *   SpO₂ ≈ 110 - 25 × RoR   (aproximación lineal para teléfonos)
     *
     * ADVERTENCIA: esta es una estimación no clínica. Los valores reales
     * requieren calibración con hardware certificado (sensor IR dedicado).
     */
    private double estimateSpO2(List<Double> red, List<Double> blue) {
        if (red.size() < 30) return 0;

        double dcRed  = average(red);
        double dcBlue = average(blue);
        if (dcRed == 0 || dcBlue == 0) return 0;

        double acRed  = standardDeviation(red);
        double acBlue = standardDeviation(blue);

        double ror = (acRed / dcRed) / (acBlue / dcBlue);

        // Curva de calibración (limitada al rango fisiológico 70–100 %)
        double spo2 = 110.0 - 25.0 * ror;
        return Math.max(70.0, Math.min(100.0, spo2));
    }

    // ─── Funciones estadísticas ───────────────────────────────────────────────

    private List<Double> zNormalize(List<Double> data) {
        double mean = average(data);
        double std  = standardDeviation(data);
        if (std == 0) return new ArrayList<>(data);
        List<Double> out = new ArrayList<>();
        for (double v : data) out.add((v - mean) / std);
        return out;
    }

    private List<Double> movingAverage(List<Double> data, int window) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            int from = Math.max(0, i - window / 2);
            int to   = Math.min(data.size() - 1, i + window / 2);
            double sum = 0;
            for (int j = from; j <= to; j++) sum += data.get(j);
            out.add(sum / (to - from + 1));
        }
        return out;
    }

    private double average(List<Double> data) {
        double sum = 0;
        for (double v : data) sum += v;
        return data.isEmpty() ? 0 : sum / data.size();
    }

    private double standardDeviation(List<Double> data) {
        double mean = average(data);
        double variance = 0;
        for (double v : data) variance += (v - mean) * (v - mean);
        return Math.sqrt(data.isEmpty() ? 0 : variance / data.size());
    }

    // ─── Hilo de fondo ────────────────────────────────────────────────────────

    private void startBackgroundThread() {
        backgroundThread  = new HandlerThread("PPGBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try { backgroundThread.join(); } catch (InterruptedException ignored) {}
            backgroundThread  = null;
            backgroundHandler = null;
        }
    }
}
