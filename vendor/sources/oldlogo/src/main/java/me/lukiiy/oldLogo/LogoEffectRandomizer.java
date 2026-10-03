package me.lukiiy.oldLogo;

import java.util.Random;

public class LogoEffectRandomizer {
    public double target;
    public double current;
    public double velocity;
    public float speed;

    public LogoEffectRandomizer(Random random, int x, int y, float speed) {
        this.target = this.current = 10 + y + random.nextDouble() * 32 + x;
        this.speed = speed;
    }

    public LogoEffectRandomizer(Random random, int x, int y) {
        this(random, x, y, 1);
    }

    public void tick() {
        this.current = this.target;

        double accel = 0.6 * speed;
        double damping = Math.pow(0.9, speed);

        if (this.target > 0) velocity -= accel;
        this.target += velocity * speed;
        velocity *= damping;

        if (this.target < 0) {
            this.target = 0;
            velocity = 0;
        }
    }
}
