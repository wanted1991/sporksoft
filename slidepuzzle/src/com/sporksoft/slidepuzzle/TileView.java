//TODO optimize drawing to only redraw the section that actually changed
package com.sporksoft.slidepuzzle;

import java.io.FileNotFoundException;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;


public class TileView extends View {
	private static final boolean DEBUG = false;
	
	public static final int SIZE_SMALL = 3;
	public static final int SIZE_MEDIUM = 4;
	public static final int SIZE_LARGE = 5;
	
	public static final int DIR_UP = 0;
	public static final int DIR_DOWN = 1;
	public static final int DIR_LEFT = 2;
	public static final int DIR_RIGHT = 3;
	
	private static final int SHADOW_RADIUS = 1;
	
	//Offset of tile from top left corner of cell
	float mOffsetX;
	float mOffsetY;
	
	//Current position on screen, used for drag events
	float mX;
	float mY;
	
	int mEmptyIndex;
	int mSelected;
	int mSize;
	int mSizeSqr;
	Tile mTiles[];
	
	int mImageSource;
	boolean mShowNumbers;
	boolean mShowOutlines;
	boolean mShowImage;
	Bitmap mBitmap;
	Bitmap mDefaultBitmap;
	
	SharedPreferences mPrefs;
	private boolean mSolved;
	boolean mBlankFirst;
	
	int mNumberColor;
	int mOutlineColor;
	Paint mPaint;
	
	Chronometer mTimer;
	int mMisplaced; // When this is equal to 0 the puzzle is won
	
    public TileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

	public TileView(Context context) {
		super(context);

		init();
	}
	
	private void init() {
	    setFocusable(true);
	    Context context = getContext();
	    mPrefs = PreferenceManager.getDefaultSharedPreferences(context); 
	    
	    mPaint = new Paint();
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(16);
        mPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));        
	}
	
	public void updateInstantPrefs() {		
		//update the preferences which should have an immediate effect
	    int bgColor = mPrefs.getInt(PuzzlePreferenceActivity.BACKGROUND_COLOR, getResources().getColor(R.drawable.default_bg_color));
	    setBackgroundColor(bgColor);
	    mTimer.setBackgroundColor(bgColor);
	    
		mShowNumbers = mPrefs.getBoolean(PuzzlePreferenceActivity.SHOW_NUMBERS, true);
		mShowOutlines = mPrefs.getBoolean(PuzzlePreferenceActivity.SHOW_BORDERS, true);
        mNumberColor = mPrefs.getInt(PuzzlePreferenceActivity.NUMBER_COLOR, getResources().getColor(R.drawable.default_fg_color));
        mOutlineColor = mPrefs.getInt(PuzzlePreferenceActivity.BORDER_COLOR, getResources().getColor(R.drawable.default_fg_color));
	    mShowImage = mPrefs.getBoolean(PuzzlePreferenceActivity.SHOW_IMAGE, true);
	    mImageSource = mPrefs.getInt(PuzzlePreferenceActivity.IMAGE_SOURCE, 0);
        mTimer.setTextColor(mPrefs.getInt(PuzzlePreferenceActivity.TIMER_COLOR, getResources().getColor(R.drawable.default_fg_color)));
	    
	    if (mPrefs.getBoolean(PuzzlePreferenceActivity.SHOW_TIMER, true)) {
	        mTimer.setVisibility(View.VISIBLE);
	    } else {
	        mTimer.setVisibility(View.GONE);
	    }
	    
	    requestLayout();
	}

    @Override 
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        Context context = getContext();
        if (w > 0 && h > 0) {
            mDefaultBitmap = getImageFromResource(context, R.drawable.default_image, w, h);

            mImageSource = mPrefs.getInt(PuzzlePreferenceActivity.IMAGE_SOURCE, 0);
            if (mImageSource == SelectImagePreference.IMAGE_CUSTOM ) {
                mBitmap = getImageFromUri(context, 
                        Uri.parse(mPrefs.getString(PuzzlePreferenceActivity.CUSTOM_PUZZLE_IMAGE, "")),
                        w, h);
            } else if (mImageSource == SelectImagePreference.IMAGE_RANDOM) {
                mBitmap = getImageFromUri(context, 
                        Uri.parse(mPrefs.getString(PuzzlePreferenceActivity.RANDOM_PUZZLE_IMAGE, "")),
                        w, h);                
            }
        }
    }
 
	public void newGame(Tile[] tiles, boolean blankFirst, Chronometer chronometer) {
        mMisplaced = 0;
	    mTimer = chronometer;
        mSelected = -1; //nothing selected to start
        mSolved = false;
        mBlankFirst = blankFirst;
        
        if (tiles == null) {
            if (mImageSource == SelectImagePreference.IMAGE_RANDOM) {
                SelectImagePreference.saveRandomImagePreference(getContext(), SelectImagePreference.getRandomImage(getContext().getContentResolver()));
                mBitmap = getImageFromUri(getContext(), 
                        Uri.parse(mPrefs.getString(PuzzlePreferenceActivity.RANDOM_PUZZLE_IMAGE, "")),
                        getWidth(), getHeight());                
            }
            if (DEBUG) {
                Log.v(getClass().getName(), "Image Source: " + mPrefs.getInt(PuzzlePreferenceActivity.IMAGE_SOURCE, 0));
                Log.v(getClass().getName(), "Rand File: " + mPrefs.getString(PuzzlePreferenceActivity.RANDOM_PUZZLE_IMAGE, ""));
                Log.v(getClass().getName(), "Custom File: " + mPrefs.getString(PuzzlePreferenceActivity.CUSTOM_PUZZLE_IMAGE, ""));
            }

            mSize = Integer.parseInt(mPrefs.getString(PuzzlePreferenceActivity.PUZZLE_SIZE, String.valueOf(SIZE_MEDIUM)));
            mSizeSqr = mSize * mSize;

            // Init array of tiles
            Random random = new Random();
            int start;
            int end;
            if (mBlankFirst) {
                mEmptyIndex = 0;
                start = 1;
                end = mSizeSqr;
            } else {
                start = 0;
                end = mSizeSqr - 1;
                mEmptyIndex = mSizeSqr - 1;
            }
            
            mTiles = new Tile[mSizeSqr];
            for (int i = start; i < end; ++i) {
                mTiles[i] = new Tile(i, random.nextInt() | 0xff000000);
            }
            
            // Mix up puzzle with valid moves only
            for (int i = 0; i < 100*mSize; ++i) {
                move(random.nextInt(4));
            }
        } else {
            mTiles = tiles;
            mSizeSqr = tiles.length;
            mSize = (int) Math.sqrt(mSizeSqr);
            countMisplaced();
            for (int i = 0; i < mSizeSqr; i++) {
                if (tiles[i] == null) {
                    mEmptyIndex = i;
                    break;
                }
            }
        }
        
        if (mMisplaced == 0) {
            onSolved();
        }
	}
	
	private void countMisplaced() {
        for (int i = 0; i < mSizeSqr; ++i) {
            if (null != mTiles[i] && mTiles[i].mNumber != i) {
                mMisplaced++;
            }
        }
        
        if (DEBUG) {
            Log.v(getClass().getName(), "mMisplaced: " + mMisplaced);
        }
	}
	
	private float getTileWidth() {
		return getWidth() / mSize;
	}
	
	private float getTileHeight() {
		return getHeight() / mSize;
	}
		
    @Override
    protected void onDraw(Canvas canvas) {       
		float tileWidth = getTileWidth();
		float tileHeight = getTileHeight();
						
    	for (int index = 0; index < mSizeSqr; ++index) {
			int i = index / mSize;
			int j = index % mSize;
			float x = tileWidth * j;
			float y = tileHeight * i;

			// if this is the empty cell do nothing			
			if (mTiles[index] == null) {
				continue;
			}
    					
			if(mSelected != -1) {
				int min = Math.min(mSelected, mEmptyIndex);
				int max = Math.max(mSelected, mEmptyIndex);
				int minX = min % mSize;
				int minY = min / mSize;
				int maxX = max % mSize;
				int maxY = max / mSize;
				
				if (i >= minY && i <= maxY && j == minX) {
					y += mOffsetY;
				}
				if (j >= minX && j <= maxX && i == minY) {
					x += mOffsetX;
				}
			}
			
			//Draw the image			
			if (mShowImage) {    
    			Bitmap toDraw = (mImageSource != SelectImagePreference.IMAGE_DEFAULT && mBitmap != null) ? mBitmap : mDefaultBitmap;
                int tileNumber = mTiles[index].mNumber;
                int xSrc = (int)((tileNumber % mSize) * tileWidth);
                int ySrc = (int)((tileNumber / mSize) * tileHeight);
                Rect src = new Rect(xSrc, ySrc, (int) (xSrc + tileWidth), (int) (ySrc + tileHeight));
                Rect dst = new Rect((int) x, (int) y, (int) (x + tileWidth), (int) (y + tileHeight));
                 
                canvas.drawBitmap(toDraw, src, dst, mPaint);
            } else {
                mPaint.setColor(mTiles[index].mColor);
                canvas.drawRect(x, y, x + tileWidth, y + tileHeight, mPaint);
            }
			
			//Drop shadow to make numbers and borders stand out
            mPaint.setShadowLayer(SHADOW_RADIUS, 1, 1, 0xff000000);
            
            //Draw the number
            if (mShowNumbers) {
	            mPaint.setColor(mNumberColor);
	            canvas.drawText(String.valueOf(mTiles[index].mNumber + 1), x + (tileWidth/2), y + (tileHeight/2), mPaint);
            }   
            
            //Draw the outline
            if (mShowOutlines) {
                float x2 = x + tileWidth-1;
                float y2 = y + tileHeight-1;
                float lines[] = {
                    x, y, x2, y,
                    x, y, x, y2,
                    x2, y, x2, y2,
                    x, y2, x2, y2
                };
                mPaint.setColor(mOutlineColor);
                canvas.drawLines(lines, mPaint);
            }
            
            // remove shadow layer for perfomance
            mPaint.setShadowLayer(0, 0, 0, 0);
        }
    }
            
    private int getCellIndex(float x, float y) {
    	float tileWidth = getTileWidth();
		float tileHeight = getTileHeight();
		
		int loc[] = new int[2];
		getLocationOnScreen(loc);


		if (DEBUG) {
			Log.v(getClass().getName(), "Index: " + 
					(int)((y - loc[1]) / tileHeight) * mSize + (int)((x -loc[0]) / tileWidth));
		}
		
        int xIndex = (int)((x - loc[0]) / tileWidth);
		int yIndex = (int)((y - loc[1]) / tileHeight);

		//clamp selection to edges of puzzle
		if (xIndex >= mSize) {
		    xIndex = mSize-1;
		}
		else if (xIndex < 0) {
		    xIndex = 0;
		}
		
        if (yIndex >= mSize) {
            yIndex = mSize-1;
        }
        else if (yIndex < 0) {
            yIndex = 0;
        }
		
		    
		return mSize * yIndex + xIndex;
    }
        
    private boolean isSelectable(int index) {
    	return (index / mSize == mEmptyIndex / mSize || index % mSize == mEmptyIndex % mSize) &&
    		index != mEmptyIndex;
    }
    
    public void move(int dir) {
        //prevent movement via dpad/trackball during touch
        if (mSelected >= 0) {
            return;
        }
        
    	int index;
    	switch(dir) {
    		case DIR_UP:
    			index = mEmptyIndex + mSize;
    			if ((index) < mSizeSqr) {
    				update(index);
    			}
    			break;
    		case DIR_DOWN:
    			index = mEmptyIndex - mSize;
    			if ((index) >= 0) {
    				update(index);
    			}
    			break;
    		case DIR_LEFT:
    			index = mEmptyIndex + 1;
    			if ((index % mSize) != 0 ) {
    				update(index);
    			}
    			break;
    		case DIR_RIGHT:
    			index = mEmptyIndex - 1;
    			if ((mEmptyIndex % mSize) != 0) {
    				update(index);
    			}
    			break;
    	}
    }
    
    private void redrawRow() {
        int h = (int)getTileHeight();
        int tileY = h * (mEmptyIndex / mSize);
        invalidate(0, tileY - SHADOW_RADIUS, getRight(), tileY + h + SHADOW_RADIUS);        
    }
    
    private void redrawColumn() {
        int w = (int)getTileWidth();
        int tileX =  w * (mEmptyIndex % mSize);
        invalidate(tileX  - SHADOW_RADIUS, 0, tileX + w + SHADOW_RADIUS, getBottom());
    }
    
    private void update(int index) {
    	if (index / mSize == mEmptyIndex / mSize) {
        	//Moving a row
    		if (mEmptyIndex < index) {
	            while (mEmptyIndex < index ) {
	                mTiles = (Tile[]) ArrayUtil.swap(mTiles, mEmptyIndex, mEmptyIndex + 1);
	                if (mTiles[mEmptyIndex].mNumber == mEmptyIndex) {
	                    mMisplaced--;
	                } else if(mTiles[mEmptyIndex].mNumber == mEmptyIndex + 1) {
	                    mMisplaced++;
	                }
	                ++mEmptyIndex;
	            }		
    		} else {
                while (mEmptyIndex > index ) {
                    mTiles = (Tile[]) ArrayUtil.swap(mTiles, mEmptyIndex, mEmptyIndex - 1);
                    if (mTiles[mEmptyIndex].mNumber == mEmptyIndex) {
                        mMisplaced--;
                    } else if(mTiles[mEmptyIndex].mNumber == mEmptyIndex - 1) {
                        mMisplaced++;
                    }
                    --mEmptyIndex;
                }
    		}
            redrawRow();
    	} else if (index % mSize == mEmptyIndex % mSize){
    		//Moving a column
    		if (mEmptyIndex < index) {
                while (mEmptyIndex < index ) {
                    mTiles = (Tile[]) ArrayUtil.swap(mTiles, mEmptyIndex, mEmptyIndex + mSize);
                    if (mTiles[mEmptyIndex].mNumber == mEmptyIndex) {
                        mMisplaced--;
                    } else if(mTiles[mEmptyIndex].mNumber == mEmptyIndex + mSize) {
                        mMisplaced++;
                    }
                    mEmptyIndex += mSize;
                }		    			
    		} else {
                while (mEmptyIndex > index ) {
                    mTiles = (Tile[]) ArrayUtil.swap(mTiles, mEmptyIndex, mEmptyIndex - mSize);
                    if (mTiles[mEmptyIndex].mNumber == mEmptyIndex) {
                        mMisplaced--;
                    } else if(mTiles[mEmptyIndex].mNumber == mEmptyIndex - mSize) {
                        mMisplaced++;
                    }
                    mEmptyIndex -= mSize;
                }			
    		}
            redrawColumn();
    	}    	
    }
    
    public void grabTile(float x, float y) {        
    	int index = getCellIndex(x, y);
    	mSelected = isSelectable(index) ? index : -1;
        
        //set coordinates to the upper left corner of the selected tile
        mX = x;
        mY = y;
        mOffsetX = 0;
        mOffsetY = 0;
        
        if (DEBUG) {
            Log.v(getClass().getName(), "Grab: " + mSelected + " at (" + x + ", " + y + ")");
        }
    }
    
    public void dropTile(float x, float y) {
        if (DEBUG) {    	
            Log.v(getClass().getName(), "Drop: " + mSelected + " at (" + x + ", " + y + ")");
        }
        
    	if (mSelected != -1 && (Math.abs(mOffsetX) > getTileWidth()/2 ||
    			Math.abs(mOffsetY) > getTileHeight()/2)) {   
    		update(mSelected);
    	} else if (mSelected % mSize == mEmptyIndex % mSize) {
            redrawColumn();
        } else if (mSelected / mSize == mEmptyIndex / mSize) {
            redrawRow();
        }
    	mSelected = -1;
    }
    
    public void dragTile(float x, float y) {
        if (mSelected < 0)
    	    return;
        
        int w = (int)getTileWidth();
        int h = (int)getTileHeight();
                
        //Only drag in a single plane, either x or y depending on location of empty cell
        //prevent tiles from being dragged onto other tiles
        if (mSelected % mSize == mEmptyIndex % mSize) {
	    	if (mSelected > mEmptyIndex) {
	            mOffsetY += y - mY;
	            if (mOffsetY > 0) {
	            	mOffsetY = 0;
	            } else if (Math.abs(mOffsetY) > h) {
	            	mOffsetY = -h;
	            }
	            mY = y;

	    	} else {
	            mOffsetY += y - mY;
	            if (mOffsetY < 0) {
	            	mOffsetY = 0;
	            } else if (mOffsetY > h) {
	            	mOffsetY = h;
	            }
	            mY = y;
	    	}
	    	redrawColumn();
    	} else if (mSelected / mSize == mEmptyIndex / mSize) {
	    	if (mSelected > mEmptyIndex) {
	            mOffsetX += x - mX;
	            if (mOffsetX > 0) {
	            	mOffsetX = 0;
	            } else if (Math.abs(mOffsetX) > w) {
	            	mOffsetX = -w;
	            }
	            mX = x;
	    	} else {
	            mOffsetX += x - mX;
	            if (mOffsetX < 0) {
	            	mOffsetX = 0;
	            } else if (mOffsetX > w) {
	            	mOffsetX = w;
	            }
	            mX = x;
	    	}
	    	redrawRow();
    	}        
    }  
    
    public static Bitmap getImageFromUri(Context context, Uri uri, int width, int height) {
        ParcelFileDescriptor pfd;
        
        try {
            pfd = context.getContentResolver().openFileDescriptor(uri, "r");
        } catch(FileNotFoundException fnfe) {
            Log.v(TileView.class.getName(), fnfe.getLocalizedMessage());
            return null;
        }
        
        //get the dimensions of the image
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, opts);
        
        // get the image and scale it appropriately
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = Math.max(opts.outWidth/width, opts.outHeight/height);
        
        Bitmap bmp = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, opts);
        if (bmp == null) {
            return null;
        }
        
        return Bitmap.createScaledBitmap(bmp,
                width, height, false);
    }
    
    public static Bitmap getImageFromResource(Context context, int resId, int width, int height) {
        Resources resources = context.getResources();
        
        BitmapFactory.decodeResource(context.getResources(), R.drawable.default_image);
        
        //get the dimensions of the image
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, R.drawable.default_image, opts);
        
        // get the image and scale it appropriately
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = Math.max(opts.outWidth/width, opts.outHeight/height);
        
        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.default_image, opts),
                width, height, false);
    }
    
    public boolean checkSolved() {
        if (DEBUG) {
            Log.v(getClass().getName(), "mMisPlaced: " + mMisplaced);
        }
        
        if (mSolved) {
            return true;
        }

        if (mMisplaced == 0) {
            onSolved();
            return true;
        }
        
        return false;
    }
    
    private void onSolved() {
        mSolved = true;
        if (mBlankFirst) {
            mTiles[0] = new Tile(0, new Random().nextInt() | 0xff000000);
        } else {
            mTiles[mSizeSqr-1] = new Tile(mSizeSqr-1, new Random().nextInt() | 0xff000000);            
        }
        invalidate();
    }
    
    public boolean isSolved() {
        return mSolved;
    }
    
    public Tile[] getTiles() {
        return mTiles;
    }
}
