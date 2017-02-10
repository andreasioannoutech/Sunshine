package com.kikkos.sunshine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by kikkos on 9/6/2016.
 */
public class MyView extends View{

    private Paint circlePaint = new Paint();
    private double circleCenterWidth;
    private double circleCenterHeight;
    private double circleRadius;
    private double windDirectionWidth;
    private double windDirectionHeight;
    private boolean enableWindIndicator;
    private boolean enableWindIcon;
    private Context mContext;
    private double mDegrees;

    public MyView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public MyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr){
        mContext = context;
        enableWindIndicator = false;
        enableWindIcon = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        circlePaint.setAntiAlias(true);

        if (enableWindIcon){
            // Draw circle
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(10);
            circlePaint.setColor(Color.BLACK);
            canvas.drawCircle((float)circleCenterWidth, (float)circleCenterHeight, (float)circleRadius, circlePaint);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                circlePaint.setColor(mContext.getColor(R.color.colorPrimaryLight));
            }else {
                circlePaint.setColor(Color.BLUE);
            }
            circlePaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle((float)circleCenterWidth, (float)circleCenterHeight, (float)circleRadius - 10, circlePaint);

            //Draw Indicator in the circle.
            if (enableWindIndicator){
                circlePaint.setStrokeWidth(6);
                circlePaint.setColor(Color.RED);
                canvas.drawLine((float)circleCenterWidth, (float)circleCenterHeight, (float)windDirectionWidth, (float)windDirectionHeight, circlePaint);
            }
        }
    }

//    @Override
//    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
//        event.getText().add(String.valueOf(mDegrees) + " degrees direction.");
//        return true;
//    }

    private void calculateCircle(){
        circleCenterWidth = this.getMeasuredWidth()/2;
        circleCenterHeight = this.getMeasuredHeight()/2;
        if (circleCenterWidth>circleCenterHeight){
            circleRadius = circleCenterHeight - 10;
        }else {
            circleRadius = circleCenterWidth - 10;
        }
    }

    public void setWindDirection(double degrees){
        // subtracting 90 degrees from the wind's direction degrees value as the circle 0 degree possition
        // is at the far east position(90 degrees), simple the degrees of the wind direction are +90 degrees on the java circle
        mDegrees = degrees;
        double convertedDegrees;
        if (degrees >= 90){
            convertedDegrees = degrees - 90;
        }else {
            double remain = 90 - degrees;
            convertedDegrees = 360 - remain;
        }
        // shortening the indicator by 10
        double radius = circleRadius - 10;
        windDirectionWidth = circleCenterWidth + radius * Math.cos(Math.toRadians(convertedDegrees));
        windDirectionHeight = circleCenterHeight + radius * Math.sin(Math.toRadians(convertedDegrees));
        enableWindIndicator = true;
        invalidate();
//        AccessibilityManager accessibilityManager = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
//        if (accessibilityManager.isEnabled()){
//            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
//        }
        this.setContentDescription(mContext.getString(R.string.details_wind_direction_icon) + mDegrees + " degrees.");
    }

    public void setEnableWindIcon(boolean setting){
        enableWindIcon = setting;
        calculateCircle();
        invalidate();
    }

}
