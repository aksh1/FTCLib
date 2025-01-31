package com.arcrobotics.ftclib.drivebase;

import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.Motor;

/**
 * This is a classfile representing the kinematics of a mecanum drivetrain
 * and controls their speed. The drive methods {@link #driveRobotCentric(double, double, double)}
 * and {@link #driveFieldCentric(double, double, double, double)} are meant to be put inside
 * of a loop. You can call them in {@code void loop()} in an OpMode and within
 * a {@code while (!isStopRequested() && opModeIsActive())} loop in the
 * {@code runOpMode()} method in LinearOpMode.
 * <p>
 * For the derivation of mecanum kinematics, please watch this video:
 * https://www.youtube.com/watch?v=8rhAkjViHEQ.
 */
public class MecanumDrive extends RobotDrive {
    private double rightSideMultiplier;

    Motor[] motors;

    /**
     * Sets up the constructor for the mecanum drive.
     * Automatically inverts right side by default
     *
     * @param frontLeft  the front left motor
     * @param frontRight the front right motor
     * @param backLeft   the back left motor
     * @param backRight  the back right motor
     */
    public MecanumDrive(Motor frontLeft, Motor frontRight, Motor backLeft, Motor backRight) {
        this(true, frontLeft, frontRight, backLeft, backRight);
    }

    /**
     * Sets up the constructor for the mecanum drive.
     *
     * @param autoInvert Whether or not to automatically invert the right motors
     * @param frontLeft  the front left motor
     * @param frontRight the front right motor
     * @param backLeft   the back left motor
     * @param backRight  the back right motor
     */
    public MecanumDrive(boolean autoInvert, Motor frontLeft, Motor frontRight, Motor backLeft, Motor backRight) {
        motors = new Motor[]{frontLeft, frontRight, backLeft, backRight};
        setRightSideInverted(autoInvert);
    }

    /**
     * Checks if the right side motors are inverted.
     *
     * @return true if the multiplier for the right side is equal to -1.
     */
    public boolean isRightSideInverted() {
        return rightSideMultiplier == -1.0;
    }

    /**
     * Sets the right side inversion factor to the specified boolean.
     *
     * @param isInverted If true, sets the right side multiplier to -1 or 1 if false.
     */
    public void setRightSideInverted(boolean isInverted) {
        rightSideMultiplier = isInverted ? -1.0 : 1.0;
    }

    /**
     * Sets the range of the input, see RobotDrive for more info.
     *
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     */
    public void setRange(double min, double max) {
        super.setRange(min, max);
    }

    /**
     * Sets the max speed of the drivebase, see RobotDrive for more info.
     *
     * @param value The maximum output speed.
     */
    public void setMaxSpeed(double value) {
        super.setMaxSpeed(value);
    }

    /**
     * Stop the motors.
     */
    @Override
    public void stop() {
        for (Motor x : motors) {
            x.stopMotor();
        }
    }


    /**
     * Drives the robot from the perspective of the robot itself rather than that
     * of the driver.
     *
     * @param strafeSpeed  the horizontal speed of the robot, derived from input
     * @param forwardSpeed the vertical speed of the robot, derived from input
     * @param turnSpeed    the turn speed of the robot, derived from input
     */
    public void driveRobotCentric(double strafeSpeed, double forwardSpeed, double turnSpeed) {
        driveFieldCentric(strafeSpeed, forwardSpeed, turnSpeed, 0.0);
    }


    /**
     * Drives the robot from the perspective of the robot itself rather than that
     * of the driver.
     *
     * @param strafeSpeed  the horizontal speed of the robot, derived from input
     * @param forwardSpeed the vertical speed of the robot, derived from input
     * @param turnSpeed    the turn speed of the robot, derived from input
     * @param squareInputs Square joystick inputs for finer control
     */
    public void driveRobotCentric(double strafeSpeed, double forwardSpeed, double turnSpeed, boolean squareInputs) {
        strafeSpeed = squareInputs ? clipRange(squareInput(strafeSpeed)) : clipRange(strafeSpeed);
        forwardSpeed = squareInputs ? clipRange(squareInput(forwardSpeed)) : clipRange(forwardSpeed);
        turnSpeed = squareInputs ? clipRange(squareInput(turnSpeed)) : clipRange(turnSpeed);

        driveRobotCentric(strafeSpeed, forwardSpeed, turnSpeed);
    }

    /**
     * Drives the robot from the perspective of the driver. No matter the orientation of the
     * robot, pushing forward on the drive stick will always drive the robot away
     * from the driver.
     *
     * @param strafeSpeed  the horizontal speed of the robot, derived from input
     * @param forwardSpeed the vertical speed of the robot, derived from input
     * @param turnSpeed    the turn speed of the robot, derived from input
     * @param gyroAngle    the heading of the robot, derived from the gyro
     */
    public void driveFieldCentric(double strafeSpeed, double forwardSpeed,
                                  double turnSpeed, double gyroAngle) {
        strafeSpeed = clipRange(strafeSpeed);
        forwardSpeed = clipRange(forwardSpeed);
        turnSpeed = clipRange(turnSpeed);

        Vector2d input = new Vector2d(strafeSpeed, forwardSpeed);
        input = input.rotateBy(-gyroAngle);

        double theta = input.angle();

        double[] wheelSpeeds = new double[4];
        wheelSpeeds[MotorType.kFrontLeft.value] = Math.sin(theta + Math.PI / 4);
        wheelSpeeds[MotorType.kFrontRight.value] = Math.sin(theta - Math.PI / 4);
        wheelSpeeds[MotorType.kBackLeft.value] = Math.sin(theta - Math.PI / 4);
        wheelSpeeds[MotorType.kBackRight.value] = Math.sin(theta + Math.PI / 4);

        normalize(wheelSpeeds, input.magnitude());

        wheelSpeeds[MotorType.kFrontLeft.value] += turnSpeed;
        wheelSpeeds[MotorType.kFrontRight.value] -= turnSpeed;
        wheelSpeeds[MotorType.kBackLeft.value] += turnSpeed;
        wheelSpeeds[MotorType.kBackRight.value] -= turnSpeed;

        normalize(wheelSpeeds);

        motors[MotorType.kFrontLeft.value]
                .set(wheelSpeeds[MotorType.kFrontLeft.value] * maxOutput);
        motors[MotorType.kFrontRight.value]
                .set(wheelSpeeds[MotorType.kFrontRight.value] * rightSideMultiplier * maxOutput);
        motors[MotorType.kBackLeft.value]
                .set(wheelSpeeds[MotorType.kBackLeft.value] * maxOutput);
        motors[MotorType.kBackRight.value]
                .set(wheelSpeeds[MotorType.kBackRight.value] * rightSideMultiplier * maxOutput);
    }
    
    /**
     * Drives the robot from the perspective of the driver. No matter the orientation of the
     * robot, pushing forward on the drive stick will always drive the robot away
     * from the driver. 
     *
     * @param joystickX  the x value of the joystick used to drive the robot, derived from input.
     * @param joystickY  the y value of the joystick used to drive the robot, derived from input.
     * @param turnSpeed  the turn speed of the robot, derived from input
     * @param heading    the heading of the robot, derived from the gyro
     */
    public void driveFieldCentric(double joystickX, double joystickY, double turnSpeed, double heading) {
        double angle = Math.toDegrees(Math.atan2(joystickY, joystickX)) + 180;
        double magnitude = Math.sqrt(Math.pow(joystickX, 2) + Math.pow(joystickY, 2));
        double absolute = angle - heading;
        //Calculate the two powers for the two pairs of wheels: FR, BL; FL, BR. Subtract the heading for field centric control.
        double v1 = magnitude * Math.sin(Math.toRadians(absolute + 45));
        double v2 = magnitude * Math.sin(Math.toRadians(absolute - 45));
        double[] powers = new double[]{
                v1 + turnSpeed,
                v2 + turnSpeed,
                v2 - turnSpeed,
                v1 - turnSpeed
        };
        //Scale the powers up/down depending on if the largest power is greater than or less than 1.
        //Scaling down is necessary because the robot won't move in the right direction if value(s) are greater than 1. Scaling up is beneficial because it maximizes how fast the robot goes.
        scale(powers);
        //Finally, set the motor speeds to the scaled powers.
        motors[MotorType.kFrontLeft.value]
                .set(powers[0]);
        motors[MotorType.kBackLeft.value]
                .set(powers[1]);
        motors[MotorType.kFrontRight.value]
                .set(powers[2]);
        motors[MotorType.kBackRight.value]
                .set(powers[3]);
    }
    
    private void scale(double[] powers) {
        double maxPower = 0;
        for (double d :
                powers) {
            maxPower = Math.max(maxPower, Math.abs(d));
        }
        if(maxPower == 0) {
            return;
        }
        else if(maxPower > 1) {
            //scale down to 1
            for (int i = 0; i < powers.length; i++) {
                powers[i] = powers[i]/maxPower;
            }
        } else {
            //scale up to 1
            for (int i = 0; i < powers.length; i++) {
                powers[i] = (1/Math.sin(Math.toRadians(135))) * powers[i];
            }
        }
    }
    
    /**
     * Drives the robot from the perspective of the driver. No matter the orientation of the
     * robot, pushing forward on the drive stick will always drive the robot away
     * from the driver.
     *
     * @param xSpeed       the horizontal speed of the robot, derived from input
     * @param ySpeed       the vertical speed of the robot, derived from input
     * @param turnSpeed    the turn speed of the robot, derived from input
     * @param gyroAngle    the heading of the robot, derived from the gyro
     * @param squareInputs Square the value of the input to allow for finer control
     */
    public void driveFieldCentric(double xSpeed, double ySpeed, double turnSpeed, double gyroAngle, boolean squareInputs) {
        xSpeed = squareInputs ? clipRange(squareInput(xSpeed)) : clipRange(xSpeed);
        ySpeed = squareInputs ? clipRange(squareInput(ySpeed)) : clipRange(ySpeed);
        turnSpeed = squareInputs ? clipRange(squareInput(turnSpeed)) : clipRange(turnSpeed);

        driveFieldCentric(xSpeed, ySpeed, turnSpeed, gyroAngle);
    }

}
