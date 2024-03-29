public class Planet {
    public int id;
    public double mass;
    public double size;

    private boolean phase;
    public double x1, y1;
    public double x2, y2;

    public double ax, ay;

    public double xVel, yVel;

    public String toString() {
        // pretty string formatter
        //return String.format("Pl-id:%d mass:%.2f p1:(%.2f,%.2f) p2:(%.2f,%.2f) vel:(%.2f,%.2f)", this.id, this.mass, this.x1, this.y1, this.x2, this.y2, this.xVel, this.yVel);
        //return String.format("p%d (%.2f,%.2f) (%.2f,%.2f)", this.id, this.x1, this.y1, this.x2, this.y2);
        return String.format("p%d (%.2f,%.2f)", this.id, this.getX(), this.getY());
    }

    public double getX() {
        if (phase) {
            return x1;
        }
        else {
            return x2;
        }
    }

    public double getY() {
        if (phase) {
            return y1;
        }
        else {
            return y2;
        }
    }

    public void setX(double newX) {
        if (phase) {
            x2 = newX;
        }
        else {
            x1 = newX;
        }
    }

    public void setY(double newY) {
        if (phase) {
            y2 = newY;
        }
        else {
            y1 = newY;
        }
    }

    // Be carefull!!!
    public void setThisXY(double newX, double newY) {
        if (phase) {
            x1 = newX;
            y1 = newY;
        }
        else {
            x2 = newX;
            y2 = newY;
        }
    }

    public void updateCoordinates() {
        // switch to new values for getX/Y and setX/Y
        this.ax = 0;
        this.ay = 0;
        this.phase = !this.phase;
    }

    public Planet(int id, double mass, double size, double x1, double y1, double xVel, double yVel) {
        this.id = id;
        this.mass = mass;
        this.size = size;
        this.phase = true;
        this.x1 = x1;
        this.y1 = y1;
        this.ax = 0;
        this.ay = 0;
        this.xVel = xVel;
        this.yVel = yVel;
    }
}
