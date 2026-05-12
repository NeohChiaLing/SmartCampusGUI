package com.neoh.smartcampusgui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class SmartCampusDashboard extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Set up the Layout (Dark Mode)
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a;"); // Dark navy background

        // 2. Create the Line Chart
        NumberAxis xAxis = new NumberAxis(0, 24, 3);
        xAxis.setLabel("Time (24 Hours)");

        NumberAxis yAxis = new NumberAxis(0, 8, 1);
        yAxis.setLabel("Energy Demand (kW)");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("SmartCampus AI Engine - Live Demand");
        lineChart.setLegendVisible(false);

        // Add the Data Points (The Spike at 15:00)
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>(0, 2.0));
        series.getData().add(new XYChart.Data<>(6, 2.5));
        series.getData().add(new XYChart.Data<>(9, 4.0));
        series.getData().add(new XYChart.Data<>(12, 4.5));

        // The Peak Data Point!
        XYChart.Data<Number, Number> peakData = new XYChart.Data<>(15, 7.5);
        series.getData().add(peakData);

        series.getData().add(new XYChart.Data<>(18, 5.0));
        series.getData().add(new XYChart.Data<>(24, 3.0));
        lineChart.getData().add(series);

        // 3. Create the Right Action Panel
        VBox actionPanel = new VBox(20);
        actionPanel.setPadding(new Insets(30));
        actionPanel.setAlignment(Pos.CENTER);
        actionPanel.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 2px; -fx-border-radius: 10px;");

        Label alertLabel = new Label("⚠️ ALERT: Peak Predicted at 15:00");
        alertLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label recommendation = new Label("💡 AI Recommendation:\nReduce HVAC in Block B by 10%");
        recommendation.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label savingsTitle = new Label("Potential Savings:");
        savingsTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        Label savingsAmount = new Label("RM 850.00");
        savingsAmount.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        savingsAmount.setStyle("-fx-text-fill: #22c55e;"); // Neon green

        Button approveButton = new Button("⚡ APPROVE SHAVING PROTOCOL");
        approveButton.setStyle("-fx-background-color: #22c55e; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 20px; -fx-background-radius: 5px;");

        // 4. THE MAGIC: What happens when you click the button
        approveButton.setOnAction(e -> {
            peakData.setYValue(4.8); // Drop the peak instantly!

            alertLabel.setText("✅ Peak Shaved Successfully");
            alertLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;");
            approveButton.setText("PROTOCOL ACTIVE");
            approveButton.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        });

        actionPanel.getChildren().addAll(alertLabel, recommendation, savingsTitle, savingsAmount, approveButton);

        // 5. Put it together
        root.setCenter(lineChart);
        root.setRight(actionPanel);
        BorderPane.setMargin(actionPanel, new Insets(20));

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("SmartCampus AI Engine");
        primaryStage.setScene(scene);

        // --- CRITICAL FIX: We MUST show the window BEFORE applying advanced styles ---
        primaryStage.show();

        // 6. Apply Advanced Styles safely
        try {
            xAxis.setStyle("-fx-tick-label-fill: white;");
            yAxis.setStyle("-fx-tick-label-fill: white;");
            series.getNode().setStyle("-fx-stroke: #22d3ee; -fx-stroke-width: 3px;");
            if (lineChart.lookup(".chart-title") != null) {
                lineChart.lookup(".chart-title").setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 18px;");
            }
        } catch (Exception e) {
            // If styling fails, the app still runs perfectly fine for the video!
            System.out.println("App launched safely.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}