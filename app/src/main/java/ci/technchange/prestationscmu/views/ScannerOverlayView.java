package ci.technchange.prestationscmu.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.List;

public class ScannerOverlayView extends View {
    private Paint paint;
    private int frameColor = Color.RED;
    private List<PointF> corners;

    public ScannerOverlayView(Context context) {
        super(context);
        init();
    }

    public ScannerOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScannerOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(frameColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10); // Épaisseur du trait
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(frameColor);

        if (corners != null && corners.size() == 4) {
            // Dessiner un polygone basé sur les coins détectés
            float[] points = new float[8];
            for (int i = 0; i < 4; i++) {
                points[i*2] = corners.get(i).x;
                points[i*2+1] = corners.get(i).y;
            }

            // Dessiner les lignes connectant les points
            canvas.drawLine(points[0], points[1], points[2], points[3], paint);
            canvas.drawLine(points[2], points[3], points[4], points[5], paint);
            canvas.drawLine(points[4], points[5], points[6], points[7], paint);
            canvas.drawLine(points[6], points[7], points[0], points[1], paint);

            // Dessiner les points aux coins
            Paint dotPaint = new Paint();
            dotPaint.setColor(frameColor);
            dotPaint.setStyle(Paint.Style.FILL);

            for (PointF corner : corners) {
                canvas.drawCircle(corner.x, corner.y, 20, dotPaint);
            }
        } else {
            // Dessiner un rectangle par défaut si aucun coin n'est détecté
            Rect rect = new Rect(0, 0, getWidth(), getHeight());
            canvas.drawRect(rect, paint);
        }
    }

    public void setFrameColor(int color) {
        this.frameColor = color;
        invalidate(); // Redessiner la vue
    }

    public void setCorners(List<PointF> corners) {
        this.corners = corners;
        invalidate(); // Redessiner la vue
    }
}
