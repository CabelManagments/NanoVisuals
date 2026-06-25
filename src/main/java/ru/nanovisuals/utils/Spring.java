package ru.nanovisuals.utils;

public class Spring {

    public float position;
    public float target;
    public float velocity;
    public float stiffness;
    public float damping;

    public Spring() {
        this(180f, 18f);
    }

    public Spring(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void snap(float v) {
        this.position = v;
        this.target = v;
        this.velocity = 0f;
    }

    public void setTarget(float v) {
        this.target = v;
    }

    public void update(float dt) {
        if (dt <= 0f) return;
        if (dt > 0.1f) dt = 0.1f;
        float accel = (target - position) * stiffness - velocity * damping;
        velocity += accel * dt;
        position += velocity * dt;
    }

    public void kick(float impulse) {
        velocity += impulse;
    }
}
