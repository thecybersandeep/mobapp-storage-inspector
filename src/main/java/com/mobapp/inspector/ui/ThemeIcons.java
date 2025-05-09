package com.mobapp.inspector.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class ThemeIcons {
    
    public static StackPane createSunIcon(double size) {
        StackPane icon = new StackPane();
        
        Circle sun = new Circle(size / 2);
        sun.setFill(Color.rgb(248, 213, 104));
        sun.setStroke(Color.rgb(230, 190, 80));
        sun.setStrokeWidth(1);
        
        for (int i = 0; i < 8; i++) {
            Line ray = new Line(0, 0, 0, size * 0.9);
            ray.setStroke(Color.rgb(248, 213, 104));
            ray.setStrokeWidth(2);
            ray.setTranslateX(0);
            ray.setTranslateY(0);
            ray.setRotate(i * 45);
            icon.getChildren().add(ray);
        }
        
        icon.getChildren().add(sun);
        return icon;
    }
    
    public static StackPane createMoonIcon(double size) {
        StackPane icon = new StackPane();
        
        Circle fullMoon = new Circle(size / 2);
        fullMoon.setFill(Color.rgb(230, 230, 230));
        fullMoon.setStroke(Color.rgb(200, 200, 200));
        fullMoon.setStrokeWidth(1);
        
        Circle shadow = new Circle(size / 2 * 0.8);
        shadow.setFill(Color.rgb(40, 40, 50));
        shadow.setTranslateX(size / 5);
        
        for (int i = 0; i < 3; i++) {
            Rectangle star = new Rectangle(2, 2);
            star.setFill(Color.WHITE);
            star.setTranslateX(-size / 3 + (i * 5));
            star.setTranslateY(-size / 3 + (i * 7));
            icon.getChildren().add(star);
        }
        
        icon.getChildren().addAll(fullMoon, shadow);
        return icon;
    }
}