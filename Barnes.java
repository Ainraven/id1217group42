// Barnes.java

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.*;

public class Barnes {

    public static void main(String[] args) {

        /*
            Reads planets from a .csv file.
            First line should be the amount of planets to read as an integer.
            Following lines should be planets of the format:
                mass,positionX,positionY,velocityX,velocityY
        */

        int gNumBodies = 4;
        int numWorkers = 1;
        double far = 100;
        int numSteps = 1000;

        int amountOfPlanets = 0;
        Planet[] planets = new Planet[0];

        if (0 < args.length) {
            try (BufferedReader buff = new BufferedReader(new FileReader(args[0]))) {
                // read the amount of planets
                String line = buff.readLine(); // does it remove the new line sign?
                amountOfPlanets = Integer.parseInt(line);

                // allocate array for planets
                planets = new Planet[amountOfPlanets];

                // read and create planets
                int id = 0;
                String[] row;
                while ((line = buff.readLine()) != null) {
                    row = line.split(",");
                    planets[id] = new Planet(id, Double.parseDouble(row[0]), Double.parseDouble(row[1]), Double.parseDouble(row[2]), Double.parseDouble(row[3]), Double.parseDouble(row[4]));
                    id++;
                }
            } catch (Exception e) {
                System.out.println("File " + args[0] + " could not be opened.");
            }
        }
        //Space space = new Space();
        System.out.println("Planets in the order they were added");
        for (Planet planet : planets) {
            System.out.println(planet.toString());
        }

        int height = 32;
        int width = 32;

        System.out.println("Height: " + height + ", Width: " + width);

        Tree tree = new Tree(height, width);
        tree.createTree(planets);

        System.out.println("\nTree");
        tree.prettyPrint();

        CyclicBarrier barrier = new CyclicBarrier(numWorkers);
        int stripSize = (gNumBodies % numWorkers == 0) ? (gNumBodies / numWorkers) : ((gNumBodies / numWorkers) + 1);
        int start;
        int end;

        // Create workers
        Worker[] workers = new Worker[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            start = i * stripSize;
            end = (i == numWorkers - 1) ? (gNumBodies - 1) : (start + stripSize - 1); // edge case. Giving the last worker extra work if the division is uneven
            workers[i] = new Worker(i, barrier, tree, planets, start, end, far);
            workers[i].start();
        }

        // Wait for workers to complete their 
        for (int i = 0; i < numWorkers; i++) {
            try{
                workers[i].join();
            } catch (InterruptedException ex){
                System.out.println("JOIN INTERRUPTED");
                return;
            }
        }

        tree.prettyPrint();

    }
    
    private static class Worker extends Thread {
        
        int id;
        CyclicBarrier barrier;
        Tree tree;
        Planet[] planets;
        int startPlanetIndex;
        int endPlanetIndex;
        double far;
        private final double gforce = 6.67 * Math.pow(10, -11);
        private final double secondsPerFrame = 1;
        
        public Worker(int id, CyclicBarrier barrier, Tree tree, Planet[] planets, int startPlanetIndex, int endPlanetIndex, double far) {
            this.id = id;
            this.barrier = barrier;
            this.tree = tree;
            this.planets = planets;
            this.startPlanetIndex = startPlanetIndex;
            this.endPlanetIndex = endPlanetIndex;
            this.far = far;
        }

        // Arguments:
        //  barrier, tree, planets, startPlanetIndex, endPlanetIndex
        // split array of planets among workers
        // for each planet assigned to the worker
        //  traverse tree and calculate forces (needs a queue or whatever)
        //  write new position
        // wait at barrier (needs barrier)

        // function(planet)
        private void calculateForce(Planet planet, Node node){
            // calculateForce takes a planet and a node,
            // calculates the force that the planet will feel from the node,
            // calculates the acceleration created by that force,
            // and adds this acceleration to a sum of accelerations that the planet feels

            /* Formulas
                Newton's first law
                    F = mass * acceleration
                Gravitational force
                    F = G * (mass1 * mass2) / distance^2
                (Gravitational acceleration)
                    a = F / mass1 = G * mass2 / distance^2
                Distance moved
                    distance = (current_velocity * time) + total_acceleration * (time^2) / 2
            */

            planet.ax += gforce * node.mass / ((planet.getX() - node.centerX)*(planet.getX() - node.centerX));
            planet.ay += gforce * node.mass / ((planet.getY() - node.centerY)*(planet.getY() - node.centerY));

        }

        private void traverseTree(Planet planet, Node node) {
            double distance;

            if (!node.hasChildren()) {
                if (node.planet.id != planet.id) {
                    return;
                }

                // Calculate the sum of accelerations acting on the planet
                calculateForce(planet, node);

                // Calculate where the planet 
                // distance = (current_velocity * time) + total_acceleration * (time^2) / 2
                double distanceX = (planet.xVel * secondsPerFrame) + planet.ax * secondsPerFrame*secondsPerFrame / 2;
                double distanceY = (planet.yVel * secondsPerFrame) + planet.ay * secondsPerFrame*secondsPerFrame / 2;

                double newX = planet.getX() + 1;//planet.getX() + distanceX;
                double newY = planet.getY() + 1;//planet.getY() + distanceY;

                planet.setX(tree.width < newX ? (tree.width - 1) : newX);
                planet.setY(tree.height < newY ? (tree.height - 1) : newY);
            }
            else {
                // calculate distance from planet to node's center of mass
                distance = Math.sqrt(Math.pow(planet.getX() - node.centerX, 2) + Math.pow(planet.getY() - node.centerY, 2));
                
                if (far < distance) {
                    // Approximate the force using this node
                    calculateForce(planet, node);
                }
                else {
                    // Continue down the tree
                    for(int i = 0; i < 4; i++){
                        traverseTree(planet, node.quadrant[i]);
                    }
                }
            }

        }
        
        public void run() {

            // Calculate force
            // for each of the worker's planets
            //  calculate next position of the planet
            for(int i = startPlanetIndex; i <= endPlanetIndex; i++){
                traverseTree(planets[i], tree.root);
            }
        
            // await other workers to finish their calculations
            try{
                barrier.await();
            } catch(InterruptedException ex) {
                System.out.println("Interrupted exception barrier.");
                return;
            } catch(BrokenBarrierException ex) {
                System.out.println("Broken barrier exception.");
                return;
            }
        
            // move planets
            // for each of the worker's planets
            //  call planet.update()
            //  (also draw on screen)
            for(int i = startPlanetIndex; i <= endPlanetIndex; i++){
                planets[i].updateCoordinates();
            }
            
        
            // wait for all planets to update
            try{
                barrier.await();
            } catch(InterruptedException ex) {
                System.out.println("Interrupted exception barrier.");
                return;
            } catch(BrokenBarrierException ex) {
                System.out.println("Broken barrier exception.");
                return;
            }

            // First worker will rebuid the tree and other will wait for it to finish
            if(id == 0){
                tree.createTree(planets);
            }

            try{
                barrier.await();
            } catch(InterruptedException ex) {
                System.out.println("Interrupted exception barrier.");
                return;
            } catch(BrokenBarrierException ex) {
                System.out.println("Broken barrier exception.");
                return;
            }
        }
    }
}

