package android.app.printerapp.viewer;

public class Geometry {
	
	 public static class Point {
		 public final float x, y, z;

	     public Point(float x, float y, float z) {
	    	 this.x = x;
	         this.y = y;
	         this.z = z;
	     }   
	 }
	 
	 public static class Box  {
		 static final int LEFT = 0;
		 static final int RIGHT = 1;
		 static final int FRONT = 2;
		 static final int BEHIND = 3;
		 static final int DOWN = 4;
		 static final int UP = 5;
		 
		 float coordBox [] = new float [6];
		 
		 public Box (float minX, float maxX, float minY, float maxY, float minZ, float maxZ ) {
			 coordBox[LEFT] = minX;
			 coordBox[RIGHT] = maxX;
			 coordBox[FRONT] = minY;
			 coordBox[BEHIND] = maxY;
			 coordBox[DOWN] = minZ;
			 coordBox[UP] = maxZ;
		 }
	 }
	 
	 public static class Vector  {
		 public final float x, y, z;

	     public Vector(float x, float y, float z) {
	    	 this.x = x;
	         this.y = y;
	         this.z = z;
	     }
	     
	     public boolean equals (Vector v) {
	    	 if (v.x == this.x && v.y == this.y && v.z == this.z) return true;
	    	 
	    	 return false;
	     }
	 }
	 
	 public static class Ray {
		 public final Point point;
	     public final Vector vector;

	     public Ray(Point point, Vector vector) {
	    	 this.point = point;
	         this.vector = vector;
	     }        
	 }
	    
	 public static boolean intersects(Box box, Ray ray) {
		 int index = 0;
		 float k;
		 float x=Float.MIN_VALUE;
		 float y=Float.MIN_VALUE;
		 float z=Float.MIN_VALUE;
		 
		 while (index< box.coordBox.length ) {
			 switch (index) {
			 case (Box.LEFT):
			 case (Box.RIGHT):
				 k = (box.coordBox[index]-ray.point.x)/ray.vector.x;
			 	 x = box.coordBox[index];
			 	 y = ray.point.y + k*ray.vector.y;
			 	 z = ray.point.z + k*ray.vector.z;	 
			 	 break;
			 case (Box.BEHIND):
			 case (Box.FRONT):
				 k = (box.coordBox[index]-ray.point.y)/ray.vector.y;
		 	 	 x = ray.point.x + k*ray.vector.x;
			 	 y = box.coordBox[index];
		 	 	 z = ray.point.z + k*ray.vector.z;	
		 	 	 break;
			 case (Box.UP):
			 case (Box.DOWN):
				 k = (box.coordBox[index]-ray.point.z)/ray.vector.z;
	 	 	 	 x = ray.point.x + k*ray.vector.x;
	 	 	 	 y = ray.point.y + k*ray.vector.y;
			 	 z = box.coordBox[index];

	 	 	 	 break;
			 }

			 //Check if (x,y,z) is a box point
			 if (x>=box.coordBox[Box.LEFT] && x<=box.coordBox[Box.RIGHT] && 
			     y>=box.coordBox[Box.FRONT] && y<=box.coordBox[Box.BEHIND] && 
				 z>=box.coordBox[Box.DOWN] && z<=box.coordBox[Box.UP]) 	
				 	return true;
			 
			 index++;					 
		 }
		 
		 return false;
	 }
	    
	 public static Vector vectorBetween(Point from, Point to) {
		 return new Vector(
				 to.x - from.x, 
				 to.y - from.y, 
				 to.z - from.z);
	 }
	 
	 public static Point intersectionPointWitboxPlate(Ray ray) {    
		 //plane is z=centerZ
		 float k = (0-ray.point.z)/ray.vector.z;
	 	 float x = ray.point.x + k*ray.vector.x;
	 	 float y = ray.point.y + k*ray.vector.y;
	 	 float z = 0;
	 	 
	 	 return new Point (x,y,z);
	 }
	  
}
