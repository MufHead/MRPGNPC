package com.muffinhead.MRPGNPC.Calculate;

public class PositionFormat {
	public static double radian(double angle) {
		return angle/180*Math.PI;
	}
	/* y轴正方向对应0度，逆时针角度 */
	public static double[] PlanePolarToCoordinate(double distance,double angle) {
		int positive_x = 0,positive_y = 0;
		double[] coor = new double[2];
		switch ((int)angle/90) {
			case 0://x- y+
				angle = 90-angle;
				positive_x--;
				positive_y++;
				break;
			case 1://x- y-
				angle = angle-90;
				positive_x--;
				positive_y--;
				break;
			case 2://x+ y-
				angle = 270-angle;
				positive_x++;
				positive_y--;
				break;
			case 3://x+ y+
				angle = angle-270;
				positive_x++;
				positive_y++;

				break;
		}
		coor[0] = distance*Math.cos(radian(angle))*positive_x;
		coor[1] = distance*Math.sin(radian(angle))*positive_y;
		return coor;
	}
	public static double sinDistance(double distance,double angle) {
		return distance*Math.sin(radian(angle));
	}
	/* pitch平视为0，仰视为-90 */
	public static double[] PolarToCoordinate(double distance,double yaw,double pitch) {
		double[] pos = new double[3],
				ppos = new double[2];
		pos[1] = -sinDistance(distance,pitch);
		//distance = Math.abs(pos[1])/Math.tan(radian(pitch));  等价于下一句
		distance = distance*Math.cos(radian(pitch));
		ppos = PlanePolarToCoordinate(distance,yaw);
		pos[0] = ppos[0];
		pos[2] = ppos[1];
		return pos;
	}
}