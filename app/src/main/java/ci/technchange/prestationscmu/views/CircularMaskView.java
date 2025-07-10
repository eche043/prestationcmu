package ci.technchange.prestationscmu.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularMaskView extends View {

    private Paint paint;
    private Path circlePath;
    private int circleSize = 300; // Taille du cercle en dp

    public CircularMaskView(Context context) {
        super(context);
        init();
    }

    public CircularMaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularMaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Configurer la peinture pour le masque
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);

        // Configurer le mode de fusion pour créer un "trou"
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Initialiser le path pour le cercle
        circlePath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Créer un calque
        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

        // Remplir tout le canvas en blanc
        canvas.drawColor(Color.WHITE);

        // Calculer le centre du cercle
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2 - dpToPx(40); // Ajuster pour avoir un peu plus haut que le centre

        // Dessiner le cercle transparent
        circlePath.reset();
        circlePath.addOval(new RectF(
                        centerX - dpToPx(circleSize/2),
                        centerY - dpToPx(circleSize/2),
                        centerX + dpToPx(circleSize/2),
                        centerY + dpToPx(circleSize/2)),
                Path.Direction.CW);

        // Dessiner le cercle avec le mode CLEAR pour créer un trou
        canvas.drawPath(circlePath, paint);

        // Restaurer le canvas
        canvas.restoreToCount(saveCount);

        // Dessiner une bordure autour du cercle
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(2));
        canvas.drawPath(circlePath, borderPaint);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Méthode pour ajuster la taille du cercle si nécessaire
    public void setCircleSize(int sizeInDp) {
        this.circleSize = sizeInDp;
        invalidate();
    }
}
