package it.feio.android.omninotes.models;

import it.feio.android.omninotes.SketchActivity;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class SketchView extends View implements OnTouchListener {

	private static final float TOUCH_TOLERANCE = 4;

	private float strokeSize = 5;
	private int strokeColor = Color.BLACK;
	private int background = Color.WHITE;
	
	private Canvas m_Canvas;
	private Path m_Path;
	private Paint m_Paint;
	private float mX, mY;
	private int width, height;

	private ArrayList<Pair<Path, Paint>> paths = new ArrayList<Pair<Path, Paint>>();
	private ArrayList<Pair<Path, Paint>> undonePaths = new ArrayList<Pair<Path, Paint>>();
	private Context mContext;
	
	public static boolean isEraserActive = false;
	

	public SketchView(Context context, AttributeSet attr) {
		super(context, attr);
		
		this.mContext = context;
		
		setFocusable(true);
		setFocusableInTouchMode(true);
		setBackgroundColor(Color.WHITE);

		this.setOnTouchListener(this);

		m_Paint = new Paint();
		m_Paint.setAntiAlias(true);
		m_Paint.setDither(true);
		m_Paint.setColor(strokeColor);
		m_Paint.setStyle(Paint.Style.STROKE);
		m_Paint.setStrokeJoin(Paint.Join.ROUND);
		m_Paint.setStrokeCap(Paint.Cap.ROUND);
		m_Paint.setStrokeWidth(strokeSize);
		
		m_Canvas = new Canvas();
		m_Path = new Path();
		Paint newPaint = new Paint(m_Paint);
		invalidate();
	}
	
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = View.MeasureSpec.getSize(widthMeasureSpec);
	    height = View.MeasureSpec.getSize(heightMeasureSpec);
	    
	    setMeasuredDimension(width, height);
	}
	
	

	public boolean onTouch(View arg0, MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			touch_start(x, y);
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			touch_move(x, y);
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			touch_up();
			invalidate();
			break;
		}
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
			for (Pair<Path, Paint> p : paths) {
				canvas.drawPath((Path) p.first, (Paint) p.second);				
			}
	}

	private void touch_start(float x, float y) {
		// Clearing undone list
		undonePaths.clear();

		if (isEraserActive) {
			m_Paint.setColor(Color.WHITE);
			m_Paint.setStrokeWidth(strokeSize);
		} else {
			m_Paint.setColor(strokeColor);
			m_Paint.setStrokeWidth(strokeSize);
		}

		Paint newPaint = new Paint(m_Paint); // Clones the mPaint object
		paths.add(new Pair<Path, Paint>(m_Path, newPaint));
		
		m_Path.reset();
		m_Path.moveTo(x, y);
		mX = x;
		mY = y;
	}

	private void touch_move(float x, float y) {
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			m_Path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
		}
	}

	private void touch_up() {
		m_Path.lineTo(mX, mY);

		// commit the path to our offscreen
		m_Canvas.drawPath(m_Path, m_Paint);

		Paint newPaint = new Paint(m_Paint); // Clones the mPaint object
		paths.add(new Pair<Path, Paint>(m_Path, newPaint));
		// kill this so we don't double draw
		m_Path = new Path();
		
		// Advice to activity
		((SketchActivity)mContext).updateRedoAlpha();	
	}

	
	/**
	 * Returns a new bitmap associated with drawed canvas
	 * @return
	 */
	public Bitmap getBitmap() {
		if (paths.size() == 0)
			return null;
		
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(background);
		Canvas canvas = new Canvas(bitmap);
		for (Pair<Path, Paint> p : paths) {
			canvas.drawPath((Path) p.first, (Paint) p.second);				
		}
		return bitmap;
	}

	public void undo() {
		if (paths.size() >= 2) { 
			undonePaths.add(paths.remove(paths.size() - 1));
			// If there is not only one path remained both touch and move paths are removed
			undonePaths.add(paths.remove(paths.size() - 1));
			((SketchActivity)mContext).updateRedoAlpha();	
			invalidate();
		}
	}

	public void redo() {
		if (undonePaths.size() > 0) {
			paths.add(undonePaths.remove(undonePaths.size() - 1));
			paths.add(undonePaths.remove(undonePaths.size() - 1));
			((SketchActivity)mContext).updateRedoAlpha();	
			invalidate();
		}
	}
	
	public int getUndoneCount() {
		return undonePaths.size();
	}

	public int getPathsCount() {
		return paths.size();
	}
	
	public int getStrokeSize(){
		return (int)Math.round(this.strokeSize);
	}
	
	public void setStrokeSize(int size){
		strokeSize = size;
	}
	
	public int getStrokeColor(){
		return this.strokeColor;
	}
	
	public void setStrokeColor(int color){
		strokeColor = color;
	}


	public void erase() {
		paths.clear();
		undonePaths.clear();
		invalidate();
	}
}