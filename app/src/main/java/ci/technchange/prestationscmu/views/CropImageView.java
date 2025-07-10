package ci.technchange.prestationscmu.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CropImageView extends View {
    private static final int CORNER_RADIUS = 30;
    private static final int TOUCH_AREA_SIZE = 50;

    private Bitmap imageBitmap;
    private RectF imageRect;
    private RectF cropRect;
    private int activePointerId = -1;
    private int activeCorner = -1;
    private PointF lastTouchPoint = new PointF();

    private Paint borderPaint;
    private Paint cornerPaint;
    private Paint overlayPaint;

    // Coins: 0 = haut-gauche, 1 = haut-droite, 2 = bas-droite, 3 = bas-gauche
    private final RectF[] cornerTouchAreas = new RectF[4];

    public CropImageView(Context context) {
        super(context);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialiser les peintures
        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(5);
        borderPaint.setAntiAlias(true);

        cornerPaint = new Paint();
        cornerPaint.setStyle(Paint.Style.FILL);
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setAntiAlias(true);

        overlayPaint = new Paint();
        overlayPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setColor(Color.parseColor("#80000000")); // Noir semi-transparent

        // Initialiser les rectangles
        imageRect = new RectF();
        cropRect = new RectF();

        for (int i = 0; i < 4; i++) {
            cornerTouchAreas[i] = new RectF();
        }
    }

    public void setImageBitmap(Bitmap bitmap) {
        this.imageBitmap = bitmap;

        // Calculer le rectangle de l'image
        if (bitmap != null) {
            float aspectRatio = (float) bitmap.getWidth() / bitmap.getHeight();

            if (getWidth() > 0 && getHeight() > 0) {
                fitImageToView();
            } else {
                // Si la vue n'est pas encore mesurée, attendre jusqu'à sa mesure
                post(this::fitImageToView);
            }
        }

        invalidate();
    }

    private void fitImageToView() {
        if (imageBitmap == null) return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float imgWidth = imageBitmap.getWidth();
        float imgHeight = imageBitmap.getHeight();

        float scale;
        float dx = 0, dy = 0;

        if (imgWidth * viewHeight > viewWidth * imgHeight) {
            // Image plus large que la vue proportionnellement
            scale = viewWidth / imgWidth;
            dy = (viewHeight - imgHeight * scale) * 0.5f;
        } else {
            // Image plus haute que la vue proportionnellement
            scale = viewHeight / imgHeight;
            dx = (viewWidth - imgWidth * scale) * 0.5f;
        }

        imageRect.set(
                dx,
                dy,
                dx + imgWidth * scale,
                dy + imgHeight * scale
        );

        // Par défaut, le cadre de recadrage correspond à l'image entière
        cropRect.set(imageRect);

        // Mettre à jour les zones tactiles des coins
        updateCornerTouchAreas();
    }

    public void setCropRect(RectF rect) {
        if (imageRect.width() > 0 && imageRect.height() > 0 && imageBitmap != null) {
            // Convertir le rectangle fourni (en coordonnées d'image) en coordonnées de vue
            float scaleX = imageRect.width() / imageBitmap.getWidth();
            float scaleY = imageRect.height() / imageBitmap.getHeight();

            cropRect.set(
                    imageRect.left + rect.left * scaleX,
                    imageRect.top + rect.top * scaleY,
                    imageRect.left + rect.right * scaleX,
                    imageRect.top + rect.bottom * scaleY
            );

            // Mettre à jour les zones tactiles des coins
            updateCornerTouchAreas();
            invalidate();
        }
    }

    public RectF getCropRect() {
        if (imageRect.width() > 0 && imageRect.height() > 0 && imageBitmap != null) {
            // Convertir le rectangle actuel (en coordonnées de vue) en coordonnées d'image
            float scaleX = imageBitmap.getWidth() / imageRect.width();
            float scaleY = imageBitmap.getHeight() / imageRect.height();

            return new RectF(
                    (cropRect.left - imageRect.left) * scaleX,
                    (cropRect.top - imageRect.top) * scaleY,
                    (cropRect.right - imageRect.left) * scaleX,
                    (cropRect.bottom - imageRect.top) * scaleY
            );
        }

        return new RectF(0, 0, imageBitmap != null ? imageBitmap.getWidth() : 0,
                imageBitmap != null ? imageBitmap.getHeight() : 0);
    }

    private void updateCornerTouchAreas() {
        // Haut-gauche
        cornerTouchAreas[0].set(
                cropRect.left - TOUCH_AREA_SIZE,
                cropRect.top - TOUCH_AREA_SIZE,
                cropRect.left + TOUCH_AREA_SIZE,
                cropRect.top + TOUCH_AREA_SIZE
        );

        // Haut-droite
        cornerTouchAreas[1].set(
                cropRect.right - TOUCH_AREA_SIZE,
                cropRect.top - TOUCH_AREA_SIZE,
                cropRect.right + TOUCH_AREA_SIZE,
                cropRect.top + TOUCH_AREA_SIZE
        );

        // Bas-droite
        cornerTouchAreas[2].set(
                cropRect.right - TOUCH_AREA_SIZE,
                cropRect.bottom - TOUCH_AREA_SIZE,
                cropRect.right + TOUCH_AREA_SIZE,
                cropRect.bottom + TOUCH_AREA_SIZE
        );

        // Bas-gauche
        cornerTouchAreas[3].set(
                cropRect.left - TOUCH_AREA_SIZE,
                cropRect.bottom - TOUCH_AREA_SIZE,
                cropRect.left + TOUCH_AREA_SIZE,
                cropRect.bottom + TOUCH_AREA_SIZE
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (imageBitmap == null) return;

        // Dessiner l'image
        canvas.save();
        canvas.clipRect(imageRect);
        canvas.drawBitmap(imageBitmap, null, imageRect, null);
        canvas.restore();

        // Dessiner l'overlay semi-transparent en 4 parties autour du cadre
        // Partie supérieure
        canvas.drawRect(
                imageRect.left,
                imageRect.top,
                imageRect.right,
                cropRect.top,
                overlayPaint);

        // Partie inférieure
        canvas.drawRect(
                imageRect.left,
                cropRect.bottom,
                imageRect.right,
                imageRect.bottom,
                overlayPaint);

        // Partie gauche
        canvas.drawRect(
                imageRect.left,
                cropRect.top,
                cropRect.left,
                cropRect.bottom,
                overlayPaint);

        // Partie droite
        canvas.drawRect(
                cropRect.right,
                cropRect.top,
                imageRect.right,
                cropRect.bottom,
                overlayPaint);

        // Dessiner le cadre de recadrage
        canvas.drawRect(cropRect, borderPaint);

        // Dessiner les coins
        canvas.drawCircle(cropRect.left, cropRect.top, CORNER_RADIUS, cornerPaint);
        canvas.drawCircle(cropRect.right, cropRect.top, CORNER_RADIUS, cornerPaint);
        canvas.drawCircle(cropRect.right, cropRect.bottom, CORNER_RADIUS, cornerPaint);
        canvas.drawCircle(cropRect.left, cropRect.bottom, CORNER_RADIUS, cornerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                lastTouchPoint.set(event.getX(0), event.getY(0));

                // Vérifier si l'utilisateur a touché un coin
                activeCorner = -1;
                for (int i = 0; i < 4; i++) {
                    if (cornerTouchAreas[i].contains(lastTouchPoint.x, lastTouchPoint.y)) {
                        activeCorner = i;
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (activePointerId != -1) {
                    int index = event.findPointerIndex(activePointerId);
                    float x = event.getX(index);
                    float y = event.getY(index);

                    float dx = x - lastTouchPoint.x;
                    float dy = y - lastTouchPoint.y;

                    // Limiter aux bornes de l'image
                    if (activeCorner != -1) {
                        moveCorner(activeCorner, dx, dy);
                    } else if (cropRect.contains(lastTouchPoint.x, lastTouchPoint.y)) {
                        // Déplacer tout le cadre
                        moveFrame(dx, dy);
                    }

                    lastTouchPoint.set(x, y);
                    updateCornerTouchAreas();
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerId = -1;
                activeCorner = -1;
                break;
        }

        return true;
    }

    private void moveCorner(int corner, float dx, float dy) {
        switch (corner) {
            case 0: // Haut-gauche
                cropRect.left = Math.max(imageRect.left, Math.min(cropRect.left + dx, cropRect.right - 100));
                cropRect.top = Math.max(imageRect.top, Math.min(cropRect.top + dy, cropRect.bottom - 100));
                break;

            case 1: // Haut-droite
                cropRect.right = Math.min(imageRect.right, Math.max(cropRect.right + dx, cropRect.left + 100));
                cropRect.top = Math.max(imageRect.top, Math.min(cropRect.top + dy, cropRect.bottom - 100));
                break;

            case 2: // Bas-droite
                cropRect.right = Math.min(imageRect.right, Math.max(cropRect.right + dx, cropRect.left + 100));
                cropRect.bottom = Math.min(imageRect.bottom, Math.max(cropRect.bottom + dy, cropRect.top + 100));
                break;

            case 3: // Bas-gauche
                cropRect.left = Math.max(imageRect.left, Math.min(cropRect.left + dx, cropRect.right - 100));
                cropRect.bottom = Math.min(imageRect.bottom, Math.max(cropRect.bottom + dy, cropRect.top + 100));
                break;
        }
    }

    private void moveFrame(float dx, float dy) {
        // Vérifier que le cadre reste dans les limites de l'image
        float newLeft = cropRect.left + dx;
        float newTop = cropRect.top + dy;
        float newRight = cropRect.right + dx;
        float newBottom = cropRect.bottom + dy;

        if (newLeft < imageRect.left) {
            float offset = imageRect.left - newLeft;
            newLeft += offset;
            newRight += offset;
        }

        if (newTop < imageRect.top) {
            float offset = imageRect.top - newTop;
            newTop += offset;
            newBottom += offset;
        }

        if (newRight > imageRect.right) {
            float offset = newRight - imageRect.right;
            newLeft -= offset;
            newRight -= offset;
        }

        if (newBottom > imageRect.bottom) {
            float offset = newBottom - imageRect.bottom;
            newTop -= offset;
            newBottom -= offset;
        }

        cropRect.set(newLeft, newTop, newRight, newBottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (imageBitmap != null) {
            fitImageToView();
        }
    }
}
