package com.perfectcorp.widget;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.perfectcorp.utility.Log;
import com.perfectcorp.utility.R;


public class PanZoomImageView extends View {
	public static final int CENTER_INSIDE = 0;
	public static final int CENTER_CROP = 1;

	private static final float DEFAULT_MAX_SCALE = 3f;
	private static final long ANIM_DURATION = 300;
	private static final String PROP_KEY_SCALE = "scale";

	private Drawable mDrawable = null;
	private int mScaleMode = CENTER_CROP;
	private float mScale = 0f;
	private float mMinScale = 0f;
	private float mMaxScale = DEFAULT_MAX_SCALE;
	private float mTranslationX = 0f;
	private float mTranslationY = 0f;

	private GestureDetector mGestureDetector;
	private ValueAnimator mCurrentAnimator = null;

	public PanZoomImageView(Context context) {
		super(context);
		init(null);
	}

	public PanZoomImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public PanZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	private void init(AttributeSet attrs) {
		Context context = getContext();

		// 1. Initial with attributes
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PanZoomImageView);

		Drawable d = a.getDrawable(R.styleable.PanZoomImageView_src);
		if (d != null) {
			setImageDrawable(d);
		}

		mScaleMode = a.getInteger(R.styleable.PanZoomImageView_scaleMode, CENTER_INSIDE);
		mMaxScale = a.getFloat(R.styleable.PanZoomImageView_maxScale, DEFAULT_MAX_SCALE);

		a.recycle();

		// 2. Initial other members
		mGestureDetector = new GestureDetector(context, mOnGestureListener);
	}

	public void setImageDrawable(Drawable drawable) {
		mDrawable = drawable;
		if (mDrawable == null)
			return;

		mDrawable.setCallback(null);
		configBounds();
	}

	private GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
		public boolean onDown(MotionEvent e) {
			return true;
		}

		public boolean onDoubleTap(MotionEvent e) {
			Log.bear(e.getX(), ", ", e.getY());
			cancelAnimation();

			if (mDrawable == null)
				return true;

			float scale;
			if (mScale >= 1f) {
				scale = mMinScale;
			} else {
				scale = mMaxScale;
			}

			Rect bound = mDrawable.getBounds();

//			mCurrentAnimator = ValueAnimator.ofFloat(mScale, scale).setDuration(ANIM_DURATION);
			mCurrentAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(PROP_KEY_SCALE, mScale, scale)).setDuration(ANIM_DURATION);
			mCurrentAnimator.addUpdateListener(mScaleUpdateListener);
			mCurrentAnimator.addListener(mAnimatorListener);
			mCurrentAnimator.start();
			return true;
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			mTranslationX -= distanceX;
			mTranslationY -= distanceY;
			configBounds();
			return true;
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return false;
		}
	};

	private ValueAnimator.AnimatorUpdateListener mScaleUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			Float scale = (Float) animation.getAnimatedValue(PROP_KEY_SCALE);
			if (scale != null)
				mScale = scale;
//			mTranslationX *= mScale;
//			mTranslationY *= mScale;
			configBounds();
		}
	};

	private Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
		@Override
		public void onAnimationStart(Animator animation) {
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			mCurrentAnimator = null;
		}

		@Override
		public void onAnimationCancel(Animator animation) {
		}

		@Override
		public void onAnimationRepeat(Animator animation) {
		}
	};

	private void cancelAnimation() {
		if (mCurrentAnimator != null) {
			mCurrentAnimator.cancel();
			mCurrentAnimator = null;
		}
	}

	private void configBounds() {
		if (mDrawable == null)
			return;

		mMinScale = 1.0f;
		int vw = getWidth();
		int vh = getHeight();
		int dw = mDrawable.getIntrinsicWidth();
		int dh = mDrawable.getIntrinsicHeight();
		float rv = (float) vw/vh;
		float rd = (float) dw/dh;

		if (mScaleMode == CENTER_INSIDE) {
			if (rv > rd) {
				// View is wider than drawable.
				if (dh > vh) {
					mMinScale = (float) vh/dh;
				}
			} else {
				// View is thinner than drawable.
				if (dw > vw) {
					mMinScale = (float) vw/dw;
				}
			}
		} else if (mScaleMode == CENTER_CROP) {
			if (rv > rd) {
				// View is wider than drawable.
				if (dw > vw) {
					mMinScale = (float) vw/dw;
				}
			} else {
				// View is thinner than drawable.
				if (dh > vh) {
					mMinScale = (float) vh/dh;
				}
			}
		}

		mScale = Math.min(Math.max(mScale, mMinScale), mMaxScale);

		dw *= mScale;
		dh *= mScale;
		int x = -dw / 2;
		int y = -dh / 2;

		if (dw > vw) {
			mTranslationX = Math.max(Math.min(-vw/2 - x, mTranslationX), vw/2 - x - dw);
		} else {
			mTranslationX = 0;
		}

		if (dh > vh) {
			mTranslationY = Math.max(Math.min(-vh/2 - y, mTranslationY), vh/2 - y - dh);
		} else {
			mTranslationY = 0;
		}

		x += mTranslationX;
		y += mTranslationY;

		mDrawable.setBounds(x, y, x + dw, y + dh);

		// Call invalidate() to trigger onDraw to apply updated bound.
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		configBounds();
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mDrawable != null) {
			int savedCount = canvas.getSaveCount();
			canvas.save();

			canvas.translate(getWidth()/2f, getHeight()/2f);
			mDrawable.draw(canvas);

			canvas.restoreToCount(savedCount);
		}
	}
}
