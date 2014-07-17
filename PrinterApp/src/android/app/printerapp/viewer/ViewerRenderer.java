package android.app.printerapp.viewer;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.app.printerapp.viewer.Geometry.*;


public class ViewerRenderer implements GLSurfaceView.Renderer {
	Context mContext;
	private static String TAG = "ViewerRenderer";
	
	public static float Z_NEAR =1f;
	public static float Z_FAR = 3000f;
	
	private static float OFFSET_HEIGHT = 1f;
	private static float OFFSET_BIG_HEIGHT = 5f;
	
	private static int mWidth;
	private static int mHeight;
	
	public static float mCameraX = 0f;
	public static float mCameraY = 0f;
	public static float mCameraZ = 0f;
	
	public static float mCenterX = 0f;
	public static float mCenterY = 0f;
	public static float mCenterZ = 0f;
	
	public static float mSceneAngleX = -40f;
	public static float mSceneAngleY = 0f;
	
	public static float RED = 0.80f;
	public static float GREEN = 0.1f;
	public static float BLUE = 0.1f;
	public static float ALPHA = 0.9f;
	
	public static final int DOWN=0;
	public static final int RIGHT=1;
	public static final int BACK=2;
	public static final int LEFT=3;
	
	public static final float LIGHT_X=2000;
	public static final float LIGHT_Y=-2000;
	public static final float LIGHT_Z=2000;
	
	public static final int NORMAL = 0;
	public static final int XRAY = 1;
	public static final int TRANSPARENT = 2;
	public static final int LAYERS = 3;
	
	private int mState;

	private StlObject mStlObject;
	private GcodeObject mGcodeObject;
	private WitboxPlate mWitboxFaceDown;
	private WitboxFaces mWitboxFaceRight;
	private WitboxFaces mWitboxFaceBack;
	private WitboxFaces mWitboxFaceLeft;
	private WitboxPlate mInfinitePlane;
	private DataStorage mData;
			
	private boolean mShowLeftWitboxFace = true;
	private boolean mShowRightWitboxFace = true;
	private boolean mShowBackWitboxFace= true;
	private boolean mShowDownWitboxFace = true;
	
	public float[] final_matrix_R_Render = new float[16];
	public float[] final_matrix_S_Render = new float[16];
	public float[] final_matrix_T_Render = new float[16];
	
	private final float[] mVPMatrix = new float[16]; //Model View Projection Matrix
	private final float[] mProjectionMatrix = new float[16];
	private final float[] mViewMatrix = new float[16];
	private final float[] mRotationMatrix = new float[16];
	private final float[] mAccumulatedRotationMatrix = new float[16];	
	private final float[] mTemporaryMatrix = new float [16];
    private final float[] invertedViewProjectionMatrix = new float[16];
    
    //Object
	private final float[] mAccumulatedRotationObjectMatrix = new float[16];
	
	private final float[] mLightVector = new float [4];
	float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
			
	private boolean mSnapShot = false;
	
	private boolean mIsStl;
	
	//Variables Touch events
	private boolean objectPressed = false;
	
	//Variables for object edition
	float mMoveX=0;
	float mMoveY=0;
	float mMoveZ=0;
	
	float mDx;
	float mDy;
	float mDz;

	private float mAdjustZ = 0;
	private float mScaleFactor=1.0f;
	private float mLastScaleFactor = 1.0f;
		
	private Vector mVector = new Vector (1,0,0); //default
	private float mRotateAngle=0;
	private float mTotalAngle=0;
	private Point mLastCenter = new Point (0,0,0);
	
	private final int INSIDE_NOT_TOUCHED = 0;
	private final int OUT = 1;
	private final int INSIDE_TOUCHED = 2;
	
	private int mStateObject;
	

	public ViewerRenderer (DataStorage data, Context context, int state, boolean doSnapshot, boolean stl) {	
		this.mData = data;
		this.mContext = context;
		this.mState = state;
		
		this.mSnapShot = doSnapshot;
		this.mIsStl = stl;
	}
	
	public void showBackWitboxFace (boolean draw) {
		mShowBackWitboxFace = draw;
	}
	
	public void showRightWitboxFace (boolean draw) {
		mShowRightWitboxFace = draw;
	}
	
	public void showLeftWitboxFace (boolean draw) {
		mShowLeftWitboxFace = draw;
	}
	
	public void showDownWitboxFace (boolean draw) {
		mShowDownWitboxFace = draw;
	}
	
	public boolean getShowRightWitboxFace () {
		return mShowRightWitboxFace;
	}
	
	public boolean getShowLeftWitboxFace () {
		return mShowLeftWitboxFace;
	}
	
	public boolean getShowDownWitboxFace () {
		return mShowDownWitboxFace;
	}
	
	public boolean getShowBackWitboxFace () {
		return mShowBackWitboxFace;
	}
	
	public void setTransparent (boolean transparent) {
		if (mStlObject!= null) mStlObject.setTransparent(transparent);
	}
	
	public void setXray (boolean xray) {
		if (mStlObject!= null) mStlObject.setXray(xray);
	}
	
	public void setRotationVector (Vector vector) {
		mVector = vector;
	}
	
	public float getFactorScale () {
		return mScaleFactor;
	}
	
	public boolean touchPoint (float x, float y) {
		Ray ray = convertNormalized2DPointToRay(x, y);
		 	 		 
        Box objectBox = new Box (mData.getMinX(), mData.getMaxX(), mData.getMinY(), mData.getMaxY(), mData.getMinZ(), mData.getMaxZ());

        // If the ray intersects (if the user touched a part of the screen that
        // intersects the stl object's bounding box), then set objectPressed =
        // true.
        objectPressed = Geometry.intersects(objectBox, ray);
        
        if (objectPressed && mStateObject == INSIDE_NOT_TOUCHED) mStateObject = INSIDE_TOUCHED;
        
        return objectPressed;
	}
	
	int count = 0;
	
	public void dragObject (float x, float y) {
		Ray ray = convertNormalized2DPointToRay(x, y);

		Point touched = Geometry.intersectionPointWitboxPlate(ray);
		
		mMoveX = touched.x ;
        mMoveY = touched.y;
                
        float dx = mMoveX-mLastCenter.x;
		float dy = mMoveY-mLastCenter.y;
		
		float maxX = mData.getMaxX() + dx;
		float maxY = mData.getMaxY() + dy;
		float minX = mData.getMinX() + dx;
		float minY = mData.getMinY() + dy;
		
		mLastCenter = new Point (mMoveX,mMoveY,mLastCenter.z);
		
		mData.setMaxX(mData.getMaxX() + dx);
		mData.setMaxY(mData.getMaxY() + dy);
		mData.setMinX(mData.getMinX() + dx);
		mData.setMinY(mData.getMinY() + dy);
		
		//We change the colour if we are outside Witbox Plate
		if (maxX>WitboxFaces.WITBOX_LONG || minX < -WitboxFaces.WITBOX_LONG || maxY>WitboxFaces.WITBOX_WITDH || minY<-WitboxFaces.WITBOX_WITDH) 
			mStateObject = OUT;
		else mStateObject = INSIDE_TOUCHED;
		
    }
	
	public void scaleObject (float f) {
		if (f>0.1 && f<10) {	
			mScaleFactor = f;
			
			float maxX = mData.getMaxX()-mLastCenter.x;
			float maxY = mData.getMaxY()-mLastCenter.y;
			float maxZ = mData.getMaxZ();
			float minX = mData.getMinX()-mLastCenter.x;
			float minY = mData.getMinY()-mLastCenter.y;
			float minZ = mData.getMinZ();
			
			maxX = (maxX+(mScaleFactor-mLastScaleFactor)*(maxX/mLastScaleFactor))+mLastCenter.x;
			maxY = (maxY+(mScaleFactor-mLastScaleFactor)*(maxY/mLastScaleFactor))+mLastCenter.y;
			maxZ = (maxZ+(mScaleFactor-mLastScaleFactor)*(maxZ/mLastScaleFactor))+mLastCenter.z;
			
			minX = (minX+(mScaleFactor-mLastScaleFactor)*(minX/mLastScaleFactor))+mLastCenter.x;
			minY = (minY+(mScaleFactor-mLastScaleFactor)*(minY/mLastScaleFactor))+mLastCenter.y;
			minZ = (minZ+(mScaleFactor-mLastScaleFactor)*(minZ/mLastScaleFactor))+mLastCenter.z;
	
			mData.setMaxX(maxX);
			mData.setMaxY(maxY);
			mData.setMaxZ(maxZ);
			
			mData.setMinX(minX);
			mData.setMinY(minY);
			mData.setMinZ(minZ);
			
			mLastScaleFactor = mScaleFactor;
			
			if (maxX>WitboxFaces.WITBOX_LONG || minX < -WitboxFaces.WITBOX_LONG 
					|| maxY>WitboxFaces.WITBOX_WITDH || minY<-WitboxFaces.WITBOX_WITDH || maxZ>WitboxFaces.WITBOX_HEIGHT) mStateObject = OUT;
			else mStateObject = INSIDE_TOUCHED;
		}
	}
	
	public void setAngleRotationObject (float angle) {
		mRotateAngle = angle;
		mTotalAngle += angle;		
	}
	
	public void resetTotalAngle() {
		mTotalAngle = 0;
	}
	
	public void refreshRotatedObjectCoordinates () {	
		if (mTotalAngle!=0) {
			mData.initMaxMin();
			float [] coordinatesArray = mData.getVertexArray();
			float x,y,z;
			
			float [] vector = new float [4];
			float [] result = new float [4];
			float [] aux = new float [16];
						
			for (int i=0; i<coordinatesArray.length/3; i+=3) {
				vector[0] = coordinatesArray[i];
				vector[1] = coordinatesArray[i+1];
				vector[2] = coordinatesArray[i+2];
				
				Matrix.setIdentityM(aux, 0);
				Matrix.multiplyMM(aux, 0, mAccumulatedRotationObjectMatrix, 0, aux, 0);
				Matrix.multiplyMV(result, 0, aux, 0, vector, 0);
								
				x = result [0];
				y = result [1];
				z = result [2];
						
				mData.adjustMaxMin(x, y, z);
			}		
						
			float maxX = mData.getMaxX();
			float minX = mData.getMinX();
			float minY = mData.getMinY();
			float maxY = mData.getMaxY();
			float maxZ = mData.getMaxZ();
			float minZ = mData.getMinZ();
			
			//We have to introduce the rest of transformations.
			maxX = maxX*mScaleFactor+mLastCenter.x;
			maxY = maxY*mScaleFactor+mLastCenter.y;
			maxZ = maxZ*mScaleFactor+mLastCenter.z;
			
			minX = minX*mScaleFactor+mLastCenter.x;
			minY = minY*mScaleFactor+mLastCenter.y;
			minZ = minZ*mScaleFactor+mLastCenter.z;	
			
			mData.setMaxX(maxX);
			mData.setMaxY(maxY);
			
			mData.setMinX(minX);
			mData.setMinY(minY);
		
			if (minZ!=0) mAdjustZ = -mData.getMinZ();

			mData.setMinZ(mData.getMinZ()+mAdjustZ);			
			mData.setMaxZ(mData.getMaxZ()+mAdjustZ);
			
			if (maxX>WitboxFaces.WITBOX_LONG || minX < -WitboxFaces.WITBOX_LONG 
					|| maxY>WitboxFaces.WITBOX_WITDH || minY<-WitboxFaces.WITBOX_WITDH || maxZ>WitboxFaces.WITBOX_HEIGHT) mStateObject = OUT;
			else mStateObject = INSIDE_TOUCHED;			
		}	
	}
	
	
	 private Ray convertNormalized2DPointToRay(float normalizedX, float normalizedY) {		 
	        // We'll convert these normalized device coordinates into world-space
	        // coordinates. We'll pick a point on the near and far planes, and draw a
	        // line between them. To do this transform, we need to first multiply by
	        // the inverse matrix, and then we need to undo the perspective divide.
	        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};
	        final float[] farPointNdc =  {normalizedX, normalizedY,  1, 1};
	        
	        final float[] nearPointWorld = new float[4];
	        final float[] farPointWorld = new float[4];
	        
	        Matrix.multiplyMV(
	            nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
	        Matrix.multiplyMV(
	            farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);

	        // Why are we dividing by W? We multiplied our vector by an inverse
	        // matrix, so the W value that we end up is actually the *inverse* of
	        // what the projection matrix would create. By dividing all 3 components
	        // by W, we effectively undo the hardware perspective divide.
	        divideByW(nearPointWorld);
	        divideByW(farPointWorld);

	        // We don't care about the W value anymore, because our points are now
	        // in world coordinates.
	        Point nearPointRay = 
	            new Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);
				
	        Point farPointRay = 
	            new Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);

	        return new Ray(nearPointRay,  Geometry.vectorBetween(nearPointRay, farPointRay));
	 }    
	 
	  private void divideByW(float[] vector) {
		  vector[0] /= vector[3];
	      vector[1] /= vector[3];
	      vector[2] /= vector[3];
	  }
	  
	  public float getWidthScreen () {
		  return mWidth;
	  }
	  
	  public float getHeightScreen () {
		  return mHeight;
	  }
	  
	  public void exitEditionMode () {
		  switch (mStateObject) {
		  case INSIDE_NOT_TOUCHED:
			  mStateObject = INSIDE_NOT_TOUCHED;
			  break;
		  case INSIDE_TOUCHED:
			  mStateObject = INSIDE_NOT_TOUCHED;
			  break;
		  case OUT: 
			  mStateObject = OUT;
			  break;
		  }
	  }
	  
	  private void setColor () {
		  switch (mStateObject) {
		  case INSIDE_NOT_TOUCHED:
			  mStlObject.setColor(StlObject.colorNormal);
			  break;
		  case INSIDE_TOUCHED:
			  mStlObject.setColor(StlObject.colorSelectedObject);
			  break;
		  case OUT: 
			  mStlObject.setColor(StlObject.colorObjectOut);
			  break;
		  }
	  }
	  
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		// Set the background frame color
		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);		
		
		// Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		
		Matrix.setIdentityM(mAccumulatedRotationMatrix, 0);
		Matrix.setIdentityM(mAccumulatedRotationObjectMatrix, 0);

		
		mLightVector[0] = LIGHT_X;
		mLightVector[1] = LIGHT_Y;
		mLightVector[2] = LIGHT_Z;

		while (!mData.isDrawEnabled()) ; //wait	
		
		if (mIsStl) mStlObject = new StlObject (mData, mContext, mState);
		else mGcodeObject = new GcodeObject (mData, mContext);
		
		if (mSnapShot) mInfinitePlane = new WitboxPlate (mContext, true);

		mWitboxFaceBack = new WitboxFaces (BACK);
		mWitboxFaceRight = new WitboxFaces (RIGHT);
		mWitboxFaceLeft = new WitboxFaces (LEFT);
		mWitboxFaceDown = new WitboxPlate (mContext, false);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		mWidth = width;
		mHeight = height;
				
		// Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        	
        // this projection matrix is applied to object coordinates		
        Matrix.perspectiveM(mProjectionMatrix, 0, 45, ratio, Z_NEAR, Z_FAR);	
        
        if (mSnapShot) {
	        float h = mData.getHeight();
	        float l = mData.getLong();
	        float w = mData.getWidth();
	        
	        l = l/ratio; //We calculate the height related to the square in the frustum with this width 
	        w = w/ratio;
	        
	        float dh = (float) (h / (Math.tan(Math.toRadians(45/2))));
	        float dl = (float) (l/ (2*Math.tan(Math.toRadians(45/2))));
	        float dw = (float) (w/ (2*Math.tan(Math.toRadians(45/2))));
	        
	        Log.i(TAG, "WIDTH " +mData.getWidth() + " HEIGHT " + mData.getHeight() + " LONG " + mData.getLong() + " dw " + dw + " dh " + dh + " dl " + dl);

	        if (dw>dh && dw>dl) mCameraZ = OFFSET_BIG_HEIGHT*h;
	        else if (dh>dl) mCameraZ = OFFSET_HEIGHT*h;
	        else mCameraZ = OFFSET_BIG_HEIGHT*h;
	        
	        dl = dl + Math.abs(mData.getMinY());
	        dw = dw + Math.abs(mData.getMinX());
	        
	        if (dw>dh && dw>dl) mCameraY = - dw;
	        else if (dh>dl) mCameraY = -dh;
	        else mCameraY = - dl;        
        } else {
        	mCameraY = -300f;
        	mCameraZ = 300f;
        }
    	
        mSceneAngleX = -40f;

	}

	@Override
	public void onDrawFrame(GL10 unused) {
		// Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        if (mIsStl) setColor();
                
		float[] vp = new float[16];
		float[] mv = new float[16];
		float[] mvp = new float[16];
		
		float[] model = new float [16];
		float[] temporary = new float[16];
		
		float[] lightPosInEyeSpace = new float[4];
		float[] lightPosInWorldSpace = new float[4];
				
	    GLES20.glEnable (GLES20.GL_BLEND);
	 	
		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, mCameraX, mCameraY, mCameraZ, mCenterX, mCenterY, mCenterZ, 0f, 0.0f, 1.0f);
        
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
                        
        //Set Identity
        Matrix.setIdentityM(mRotationMatrix, 0);
                
        //Rotation x       
        Matrix.rotateM(mRotationMatrix, 0, mSceneAngleX, 0.0f, 0.0f, 1.0f);
        
        //RotationY
        Matrix.rotateM(mRotationMatrix, 0, mSceneAngleY, 1.0f, 0.0f, 0.0f);
        
        //Reset angle, we store the rotation in the matrix
        mSceneAngleX=0;
        mSceneAngleY=0;
        
        //Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
        Matrix.multiplyMM(mTemporaryMatrix, 0, mRotationMatrix, 0, mAccumulatedRotationMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotationMatrix, 0, 16);
        
        // Rotate the object taking the overall rotation into account.
        
        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
  
        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(vp, 0,mVPMatrix, 0, mAccumulatedRotationMatrix, 0);         
        
        Matrix.invertM(invertedViewProjectionMatrix, 0, vp, 0);
      
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, mMoveX, mMoveY, mMoveZ);  
        Matrix.scaleM(model, 0, mScaleFactor, mScaleFactor, mScaleFactor);
        
        Matrix.translateM(model, 0, 0, 0, mAdjustZ);

        //Object rotation  
        Matrix.rotateM(mAccumulatedRotationObjectMatrix, 0, mRotateAngle, mVector.x, mVector.y, mVector.z);

        //Reset angle, we store the rotation in the matrix
        mRotateAngle=0;
                
        //Multiply the model by the accumulated rotation
        Matrix.multiplyMM(temporary, 0, model, 0, mAccumulatedRotationObjectMatrix, 0);     
             
        Matrix.multiplyMM(mvp, 0,vp, 0, temporary, 0);   
            
        //Set Light direction  
        Matrix.multiplyMV(lightPosInWorldSpace, 0, temporary, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(lightPosInEyeSpace, 0, mViewMatrix, 0, lightPosInWorldSpace, 0);                                             
                                                                                                                                                                  
        if (mIsStl) mStlObject.draw(mvp, mvp, lightPosInEyeSpace);
        else mGcodeObject.draw(vp);
        
        if (mSnapShot) {
        	mInfinitePlane.draw(vp, mv);
        	takeScreenShot(unused);
        } else {
        	if (mShowDownWitboxFace) mWitboxFaceDown.draw(vp, mv);      
        	if (mShowBackWitboxFace) mWitboxFaceBack.draw(vp);
        	if (mShowRightWitboxFace) mWitboxFaceRight.draw(vp);
        	if (mShowLeftWitboxFace) mWitboxFaceLeft.draw(vp);
        }        
	}
	
	private void takeScreenShot (GL10 unused) {	
    	Log.i(TAG, "TAKING SNAPSHOT");
		int minX = 0;
		int minY = 0; 
        
        int screenshotSize = mWidth * mHeight;
        ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
        bb.order(ByteOrder.nativeOrder());
        

        GLES20.glReadPixels(minX, minY, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
        int pixelsBuffer[] = new int[screenshotSize];
        bb.asIntBuffer().get(pixelsBuffer);
        bb = null;
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixelsBuffer, screenshotSize-mWidth, -mWidth, 0, 0, mWidth, mHeight);
        pixelsBuffer = null;

        short sBuffer[] = new short[screenshotSize];
        ShortBuffer sb = ShortBuffer.wrap(sBuffer);
        bitmap.copyPixelsToBuffer(sb);

        //Making created bitmap (from OpenGL points) compatible with Android bitmap
        for (int i = 0; i < screenshotSize; ++i) {                  
            short v = sBuffer[i];
            sBuffer[i] = (short) (((v&0x1f) << 11) | (v&0x7e0) | ((v&0xf800) >> 11));
        }
        sb.rewind();
        bitmap.copyPixelsFromBuffer(sb);
        
        try {
            FileOutputStream fos = new FileOutputStream(mData.getPathSnapshot());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }      
	}
	
	public void setLightVector (float dx, float dy) {		
		mLightVector[0]=dx*LIGHT_X;
		mLightVector[2]=dy*LIGHT_Z;

	}
	
	public void setSceneAngleX (float x) {
		mSceneAngleX += x;	
	}
	
	public void setSceneAngleY (float y) {
		mSceneAngleY += y;
	}
	
	public void setCameraPosX (float x) {
		mCameraX = x;
	}
	
	public void setCameraPosY (float y) {
		mCameraY = y;
	}
	
	public void setCameraPosZ (float z) {
		mCameraZ = z;
	}
	
	public float getCameraPosX () {
		return mCameraX;
	}
	
	public float getCameraPosY () {
		return mCameraY;
	}
	
	public float getCameraPosZ () {
		return mCameraZ;
	}
	
	public void setCenterX (float x) {
		mCenterX += x;
	}
	
	public void setCenterY (float y) {
		mCenterY += y;
	}
	
	public void setCenterZ (float z) {
		mCenterZ += z;
	}
	
	public void setZNear (float h) {
		double ang = Math.toRadians(45/2);
		float valor = (float) Math.tan(ang);
		
		Z_NEAR = valor*(h/2); 
	}
	
	
	   /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
    
    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
     public static void checkGlError(String glOperation) {
         int error;
         while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
             Log.e(TAG, glOperation + ": glError " + error);
             throw new RuntimeException(glOperation + ": glError " + error);
         }
     }
}