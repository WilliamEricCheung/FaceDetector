package tech.wec.FaceDetector.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import tech.wec.FaceDetector.R;

public class RoundCornerButton extends AppCompatButton {

    private int colorNormal;
    private int colorDisabled;
    private float cornerRadius;

    private RoundCornerDrawable bgDrawableNormal = null;
    private RoundCornerDrawable bgDrawablePressed = null;
    private RoundCornerDrawable bgDrawableDisabled = null;

    public RoundCornerButton(Context context) {
        super(context);
    }

    public RoundCornerButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundCornerButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCornerBackground(attrs, defStyleAttr);
    }

    private void initCornerBackground(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RoundCornerButton, defStyleAttr, 0);

        this.cornerRadius = a.getDimension(R.styleable.RoundCornerButton_rcb_cornerRadius, 0);
        this.colorNormal = a.getColor(R.styleable.RoundCornerButton_rcb_backgroundColor, 0);
        this.colorDisabled = a.getColor(R.styleable.RoundCornerButton_rcb_backgroundColorDisabled, this.colorNormal);
        makeBackgroundDrawable();

        a.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (bgDrawableNormal != null) {
            bgDrawableNormal.setRect(right - left, bottom - top);
        }
        if (bgDrawablePressed != null) {
            bgDrawablePressed.setRect(right - left, bottom - top);
        }
        if (bgDrawableDisabled != null) {
            bgDrawableDisabled.setRect(right - left, bottom - top);
        }
    }

    public void makeBackgroundDrawable() {
        bgDrawableNormal = new RoundCornerDrawable(this.colorNormal, this.cornerRadius);
        bgDrawableNormal.setRect(getWidth(), getHeight());

        int alpha = 80;
        int colorPressed = alpha << 24 | (0x00ffffff & this.colorNormal);
        bgDrawablePressed = new RoundCornerDrawable(colorPressed, this.cornerRadius);
        bgDrawablePressed.setRect(getWidth(), getHeight());

        bgDrawableDisabled = new RoundCornerDrawable(this.colorDisabled, this.cornerRadius);
        bgDrawableDisabled.setRect(getWidth(), getHeight());

        StateListDrawable bgDrawable = new StateListDrawable();
        bgDrawable.addState(new int[]{android.R.attr.state_enabled, -android.R.attr.state_pressed}, bgDrawableNormal);
        bgDrawable.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, bgDrawablePressed);
        bgDrawable.addState(new int[]{-android.R.attr.state_enabled}, bgDrawableDisabled);
        setBackgroundDrawable(bgDrawable);
    }

    private static class RoundCornerDrawable extends Drawable {

        final int color;
        final float radius;
        final Paint paint;

        private RectF rectF;

        RoundCornerDrawable(int color, float radius) {
            this.color = color;
            this.radius = radius;

            this.paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            paint.setColor(color);

            this.rectF = new RectF();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (radius == 0) {
                canvas.drawRect(rectF, paint);
            } else {
                canvas.drawRoundRect(rectF, radius, radius, paint);
            }
        }

        @Override
        public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        public void setRect(int width, int height) {
            this.rectF.left = 0;
            this.rectF.top = 0;
            this.rectF.right = width;
            this.rectF.bottom = height;
        }
    }
}
