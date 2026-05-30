package com.neoh.smartcampusgui;


import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;


import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;


public class SmartCampusDashboard extends Application {


    private double totalRmSaved = 0.0;
    private double currentThreshold = 7.5;
    private double currentTemp = 32.0;
    private double currentOccupancy = 70.0;
    private double activeReductionsMW = 0.0;


    private String executedProtocolsHtml = "";
    private String diagnosticHtmlTable = "";
    private boolean bessDeployed = false;


    // Idea 2: Predictive Maintenance Variables
    private boolean anomalyDetected = false;
    private Label demoHealthLabel;


    private ComboBox<String> scenarioBox;
    private BorderPane root;
    private BorderPane dashboardCenter;
    private VBox dashboardRight;
    private List<Button> navButtons = new ArrayList<>();


    // Dynamic Graph & KPI Elements
    private XYChart.Series<Number, Number> demandSeries;
    private XYChart.Series<Number, Number> limitSeries;
    private XYChart.Series<Number, Number> baselineSeries;
    private XYChart.Series<Number, Number> solarSeries;


    private Label statusVal;
    private TextArea logArea;
    private Label rmSavedLabel;
    private VBox actionPanelContainer;
    private VBox cachedAssetsView;
    private ToggleButton autoPilotToggle;


    // Live KPI HUD Labels
    private Label kpiLoadLabel;
    private Label kpiCarbonLabel;
    private Label kpiCostLabel;
    private Label kpiSolarLabel;
    private Label kpiBatteryLabel;


    // Assets Elements
    private List<ProgressBar> assetBars = new ArrayList<>();
    private List<Label> assetLoadLabels = new ArrayList<>();
    private Random random = new Random();


    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #020617; -fx-font-family: 'Segoe UI', Arial, sans-serif;");


        // ================= 1. HEADER =================
        HBox header = new HBox(30);
        header.setPadding(new Insets(15, 40, 15, 40));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #0f172a; -fx-border-color: #1e293b; -fx-border-width: 0 0 2px 0;");


        statusVal = new Label("🟢 GRID SECURE & STABLE");
        statusVal.setStyle("-fx-text-fill: #10b981; -fx-font-size: 18px; -fx-font-weight: 900;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);


        Label timeLabel = new Label();
        timeLabel.setStyle("-fx-text-fill: #10b981; -fx-font-family: 'Consolas'; -fx-font-size: 16px; -fx-font-weight: bold;");


        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLabel.setText("LIVE SYNC: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
            simulateAssetFluctuations();
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();


        header.getChildren().addAll(statusVal, spacer, timeLabel);
        root.setTop(header);


        // ================= 2. SIDEBAR =================
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(30, 15, 30, 15));
        sidebar.setStyle("-fx-background-color: #0b1120; -fx-border-color: #1e293b; -fx-border-width: 0 2px 0 0;");
        sidebar.setPrefWidth(240);
        Label logo = new Label("⚡ SMART\n   CAMPUS");
        logo.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 28px; -fx-font-weight: 900;");


        Button btnDash = createNavButton("📊 Executive Dashboard");
        Button btnAssets = createNavButton("🏢 IoT Building Assets");
        Button btnROI = createNavButton("📈 ESG Financial ROI");
        Button btnSettings = createNavButton("⚙️ System Configurations");


        btnDash.setOnAction(e -> { setActiveNav(btnDash); root.setCenter(dashboardCenter); root.setRight(dashboardRight); });
        btnAssets.setOnAction(e -> { setActiveNav(btnAssets); root.setCenter(cachedAssetsView); root.setRight(null); });
        btnROI.setOnAction(e -> { setActiveNav(btnROI); root.setCenter(createROIView()); root.setRight(null); });
        btnSettings.setOnAction(e -> { setActiveNav(btnSettings); root.setCenter(createSettingsView()); root.setRight(null); });


        sidebar.getChildren().addAll(logo, new Separator(), btnDash, btnAssets, btnROI, btnSettings);
        root.setLeft(sidebar);


        // ================= 3. INITIALIZE SYSTEM =================
        buildDashboardView();
        cachedAssetsView = createAssetsView();
        updateGraphAlgorithm();


        setActiveNav(btnDash);
        root.setCenter(dashboardCenter);
        root.setRight(dashboardRight);


        Scene scene = new Scene(root, 1440, 850);


        String customCSS = "data:text/css," +
                ".chart-plot-background { -fx-background-color: transparent; }" +
                ".chart-vertical-grid-lines { -fx-stroke: #1e293b; -fx-stroke-dash-array: 4 4; }" +
                ".chart-horizontal-grid-lines { -fx-stroke: #1e293b; -fx-stroke-dash-array: 4 4; }" +
                ".axis { -fx-tick-label-fill: #64748b; -fx-font-family: 'Consolas'; }" +
                ".default-color0.chart-series-area-fill { -fx-fill: transparent; }" +
                ".default-color0.chart-series-line { -fx-stroke: #ef4444; -fx-stroke-width: 2px; -fx-stroke-dash-array: 10 10; }" +
                ".default-color1.chart-series-area-fill { -fx-fill: rgba(100, 116, 139, 0.15); }" +
                ".default-color1.chart-series-line { -fx-stroke: #64748b; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5; }" +
                ".default-color2.chart-series-area-fill { -fx-fill: linear-gradient(to bottom, rgba(56,189,248,0.5), transparent); }" +
                ".default-color2.chart-series-line { -fx-stroke: #38bdf8; -fx-stroke-width: 3px; }" +
                ".default-color3.chart-series-area-fill { -fx-fill: linear-gradient(to bottom, rgba(250,204,21,0.2), transparent); }" +
                ".default-color3.chart-series-line { -fx-stroke: #facc15; -fx-stroke-width: 2px; -fx-stroke-dash-array: 6 4; }" +
                ".text-area { -fx-background-color: #020617; -fx-text-fill: #10b981; -fx-font-family: 'Consolas'; -fx-border-color: #1e293b; -fx-border-width: 2px;}" +
                ".progress-bar .track { -fx-background-color: #1e293b; -fx-background-radius: 5; }" +
                ".progress-bar .bar { -fx-background-color: linear-gradient(to right, #0ea5e9, #10b981); -fx-background-radius: 5; }";
        scene.getStylesheets().add(customCSS);


        primaryStage.setTitle("SmartCampus - Commercial Microgrid AI Engine v13.0");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private void resetScenarioState() {
        activeReductionsMW = 0.0;
        totalRmSaved = 0.0;
        executedProtocolsHtml = "";
        diagnosticHtmlTable = "";
        bessDeployed = false;
        anomalyDetected = false; // Reset anomaly


        if (rmSavedLabel != null) rmSavedLabel.setText("RM 0.00");
        if (kpiBatteryLabel != null) {
            kpiBatteryLabel.setText("100% [IDLE]");
            kpiBatteryLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 20px; -fx-font-weight: 900; -fx-font-family: 'Consolas';");
        }


        // Reset the Health Label visually
        if (demoHealthLabel != null) {
            demoHealthLabel.setText("⚙️ Health: 98% [OPTIMAL]");
            demoHealthLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: bold;");
        }


        updateGraphAlgorithm();
    }


    // ================= 1. LIVE DASHBOARD =================
    private void buildDashboardView() {
        dashboardCenter = new BorderPane();
        VBox topContainer = new VBox(15);
        topContainer.setPadding(new Insets(20, 40, 0, 40));


        HBox kpiHUD = new HBox(15);
        kpiHUD.setAlignment(Pos.CENTER_LEFT);


        VBox kpi1 = createKPICard("GRID LOAD (TNB)", "0.00 MW", "#38bdf8", 160); kpiLoadLabel = (Label) kpi1.getChildren().get(1);
        VBox kpi2 = createKPICard("MD PENALTY RATE", "RM 97.06", "#ef4444", 160); kpiCostLabel = (Label) kpi2.getChildren().get(1);
        VBox kpi3 = createKPICard("CO2 EMISSION", "0.0 Ton/H", "#f59e0b", 160); kpiCarbonLabel = (Label) kpi3.getChildren().get(1);
        VBox kpi4 = createKPICard("SOLAR PV YIELD", "0.00 MW", "#facc15", 160); kpiSolarLabel = (Label) kpi4.getChildren().get(1);
        VBox kpi5 = createKPICard("BESS BATTERY", "100% [IDLE]", "#10b981", 160); kpiBatteryLabel = (Label) kpi5.getChildren().get(1);


        kpiHUD.getChildren().addAll(kpi1, kpi4, kpi5, kpi3, kpi2);


        VBox envControls = new VBox(15);
        envControls.setPadding(new Insets(15, 0, 10, 0));


        HBox row1 = new HBox(15); row1.setAlignment(Pos.CENTER_LEFT);
        Label scnLbl = new Label("Campus Scenario:"); scnLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); scnLbl.setMinWidth(Region.USE_PREF_SIZE);
        scenarioBox = new ComboBox<>();
        scenarioBox.getItems().addAll("Normal Semester", "Exam Week (Library Day Peak)", "Study Week (Inasis Night Peak)");
        scenarioBox.setValue("Normal Semester");
        scenarioBox.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #38bdf8; -fx-border-radius: 4;");
        scenarioBox.setOnAction(e -> resetScenarioState());
        row1.getChildren().addAll(scnLbl, scenarioBox);


        HBox row2 = new HBox(20); row2.setAlignment(Pos.CENTER_LEFT);
        Label tempLbl = new Label("Temp: 32.0°C"); tempLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); tempLbl.setMinWidth(110);
        Slider tempSlider = new Slider(24.0, 45.0, 32.0); tempSlider.setPrefWidth(200);
        tempSlider.valueProperty().addListener((obs, oldV, newV) -> {
            currentTemp = newV.doubleValue(); tempLbl.setText(String.format("Temp: %.1f°C", currentTemp));
            resetScenarioState();
        });


        Label occLbl = new Label("Occupancy: 70%"); occLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); occLbl.setMinWidth(120);
        Slider occSlider = new Slider(10.0, 100.0, 70.0); occSlider.setPrefWidth(200);
        occSlider.valueProperty().addListener((obs, oldV, newV) -> {
            currentOccupancy = newV.doubleValue(); occLbl.setText(String.format("Occupancy: %.0f%%", currentOccupancy));
            resetScenarioState();
        });
        row2.getChildren().addAll(tempLbl, tempSlider, new Separator(), occLbl, occSlider);


        envControls.getChildren().addAll(row1, row2);
        topContainer.getChildren().addAll(kpiHUD, envControls);
        dashboardCenter.setTop(topContainer);


        NumberAxis xAxis = new NumberAxis(0, 24, 2);
        NumberAxis yAxis = new NumberAxis(0, 12, 1);
        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setAnimated(false); chart.setLegendVisible(false);


        limitSeries = new XYChart.Series<>();
        baselineSeries = new XYChart.Series<>();
        demandSeries = new XYChart.Series<>();
        solarSeries = new XYChart.Series<>();


        chart.getData().addAll(limitSeries, baselineSeries, demandSeries, solarSeries);


        VBox logBox = new VBox(5);
        logBox.setPadding(new Insets(10, 40, 30, 40));
        logArea = new TextArea(); logArea.setEditable(false); logArea.setPrefHeight(120);
        logArea.setText("[SYSTEM] SmartCampus Microgrid AI Online.\n[ALGORITHM] Syncing Solar PV, BESS status, and TNB Telemetry...\n");
        Label logTitle = new Label("> SYSTEM EVENT TELEMETRY"); logTitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        logBox.getChildren().addAll(logTitle, logArea);
        dashboardCenter.setCenter(chart); dashboardCenter.setBottom(logBox);


        dashboardRight = new VBox(20);
        dashboardRight.setPadding(new Insets(25)); dashboardRight.setPrefWidth(400);
        dashboardRight.setStyle("-fx-background-color: #080f1e; -fx-border-color: #1e293b; -fx-border-width: 0 0 0 2px;");


        HBox actionHeader = new HBox(); actionHeader.setAlignment(Pos.CENTER_LEFT);
        Label actionTitle = new Label("AI SHAVING PROTOCOLS"); actionTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region headerSpacer = new Region(); HBox.setHgrow(headerSpacer, Priority.ALWAYS);


        autoPilotToggle = new ToggleButton("AUTOPILOT: OFF");
        autoPilotToggle.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-cursor: hand;");
        autoPilotToggle.setOnAction(e -> {
            if(autoPilotToggle.isSelected()) {
                autoPilotToggle.setText("AUTOPILOT: ON");
                autoPilotToggle.setStyle("-fx-background-color: #10b981; -fx-text-fill: #020617; -fx-font-weight: bold; -fx-cursor: hand;");
                logArea.appendText("\n[SYSTEM] AUTOPILOT ENGAGED. AI WILL AUTONOMOUSLY EXECUTE PROTOCOLS.");
                updateGraphAlgorithm();
            } else {
                autoPilotToggle.setText("AUTOPILOT: OFF");
                autoPilotToggle.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-cursor: hand;");
                logArea.appendText("\n[SYSTEM] AUTOPILOT DISENGAGED. MANUAL OVERRIDE REQUIRED.");
            }
        });
        actionHeader.getChildren().addAll(actionTitle, headerSpacer, autoPilotToggle);


        VBox statsBox = new VBox(5); statsBox.setAlignment(Pos.CENTER);
        statsBox.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-radius: 8; -fx-padding: 20;");
        rmSavedLabel = new Label("RM 0.00"); rmSavedLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 32px; -fx-font-weight: bold;");
        Label tLabel = new Label("MONTHLY MD SAVINGS (RM 97.06/kW):"); tLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 11px;");
        statsBox.getChildren().addAll(tLabel, rmSavedLabel);


        actionPanelContainer = new VBox(15);
        ScrollPane scrollPane = new ScrollPane(actionPanelContainer);
        scrollPane.setFitToWidth(true); scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);


        dashboardRight.getChildren().addAll(actionHeader, statsBox, new Separator(), scrollPane);
    }


    private VBox createKPICard(String title, String value, String color, double width) {
        VBox box = new VBox(5); box.setPrefWidth(width);
        box.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-radius: 8; -fx-padding: 15;");
        Label t = new Label(title); t.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label v = new Label(value); v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 20px; -fx-font-weight: 900; -fx-font-family: 'Consolas';");
        box.getChildren().addAll(t, v); return box;
    }


    private void updateGraphAlgorithm() {
        demandSeries.getData().clear();
        baselineSeries.getData().clear();
        limitSeries.getData().clear();
        solarSeries.getData().clear();
        limitSeries.getData().addAll(new XYChart.Data<>(0, currentThreshold), new XYChart.Data<>(24, currentThreshold));


        double maxPeak = 0; double currentHourLoad = 0; double currentHourSolar = 0;
        String currentScenario = scenarioBox.getValue();
        double targetPeakHour = 14.0;
        if (currentScenario.contains("Night")) targetPeakHour = 21.0;
        if (currentScenario.contains("Library")) targetPeakHour = 15.0;


        for (int i = 0; i <= 24; i++) {
            double baseLoad = 2.0; double rawLoad = baseLoad;


            if (currentScenario.equals("Normal Semester")) {
                double peakMultiplier = Math.exp(-Math.pow(i - 14, 2) / 10);
                rawLoad = baseLoad + ((currentTemp - 24) * 0.4) * (currentOccupancy / 75.0) * peakMultiplier;
            } else if (currentScenario.contains("Library Day Peak")) {
                double peakMultiplier = Math.exp(-Math.pow(i - 14, 2) / 15);
                rawLoad = baseLoad + ((currentTemp - 24) * 0.45) * (currentOccupancy / 80.0) * peakMultiplier + 0.5;
            } else if (currentScenario.contains("Inasis Night Peak")) {
                double dayMultiplier = Math.exp(-Math.pow(i - 14, 2) / 10) * 0.5;
                double nightMultiplier = Math.exp(-Math.pow(i - 21, 2) / 8);
                rawLoad = baseLoad + ((currentTemp - 24) * 0.2) * dayMultiplier + (currentOccupancy / 70.0) * 4.8 * nightMultiplier;
            }


            double solarGen = Math.max(0, 1.8 * Math.exp(-Math.pow(i - 13, 2) / 6));
            solarSeries.getData().add(new XYChart.Data<>(i, solarGen));


            double baseGridLoad = Math.max(baseLoad, rawLoad - solarGen);
            baselineSeries.getData().add(new XYChart.Data<>(i, baseGridLoad));


            double dynamicGridLoad = baseGridLoad;


            if (i >= targetPeakHour - 3 && i <= targetPeakHour + 3) {
                dynamicGridLoad = Math.max(baseLoad, baseGridLoad - activeReductionsMW);
            }


            if (bessDeployed && i >= 14 && i <= 22) {
                dynamicGridLoad = Math.max(baseLoad, dynamicGridLoad - 1.5);
            }


            demandSeries.getData().add(new XYChart.Data<>(i, dynamicGridLoad));


            if(dynamicGridLoad > maxPeak) maxPeak = dynamicGridLoad;


            if(i == 15) {
                currentHourLoad = dynamicGridLoad;
                currentHourSolar = solarGen;
            }
        }


        if(kpiLoadLabel != null) {
            kpiLoadLabel.setText(String.format("%.2f MW", currentHourLoad));
            kpiSolarLabel.setText(String.format("%.2f MW", currentHourSolar));
            kpiCarbonLabel.setText(String.format("%.1f Ton/H", currentHourLoad * 0.7));
        }


        // Show Predictive Maintenance Alert if triggered
        if (anomalyDetected) {
            statusVal.setText("⚠️ WARNING: HARDWARE ANOMALY DETECTED");
            statusVal.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 18px; -fx-font-weight: 900;");


            // Only generate new protocol if none active
            if (activeReductionsMW == 0 && !bessDeployed) {
                generateContextAwareProtocols(currentScenario, maxPeak);
            }
        }
        else if (maxPeak > currentThreshold) {
            statusVal.setText("⚠️ CRITICAL: ETOU PENALTY RISK");
            statusVal.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 18px; -fx-font-weight: 900;");


            if (activeReductionsMW == 0 && !bessDeployed) {
                generateContextAwareProtocols(currentScenario, maxPeak);
            }
            if (autoPilotToggle.isSelected()) triggerAutoPilotSequence();
        }
        else {
            statusVal.setText("🟢 GRID SECURE & STABLE");
            statusVal.setStyle("-fx-text-fill: #10b981; -fx-font-size: 18px; -fx-font-weight: 900;");
            if (activeReductionsMW == 0 && !bessDeployed) {
                actionPanelContainer.getChildren().clear();
                Label noAction = new Label("✅ Grid & Microgrid optimized.\nNo active interventions required.");
                noAction.setStyle("-fx-text-fill: #10b981; -fx-font-size: 14px; -fx-font-weight: bold;");
                actionPanelContainer.getChildren().add(noAction);
                diagnosticHtmlTable = "";
            }
        }
    }


    private void triggerAutoPilotSequence() {
        if(actionPanelContainer.getChildren().isEmpty()) return;
        for(javafx.scene.Node node : actionPanelContainer.getChildren()) {
            if(node instanceof VBox) {
                Button btn = (Button) ((VBox)node).getChildren().get(2);
                if(!btn.isDisabled()) {
                    PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
                    pause.setOnFinished(e -> btn.fire());
                    pause.play();
                    break;
                }
            }
        }
    }


    private void generateContextAwareProtocols(String scenario, double peakMW) {
        actionPanelContainer.getChildren().clear();
        logArea.appendText("\n\n[AI_ENGINE] Grid limit exceeded. Initiating diagnostics & BESS logic...");


        double dkgLoad = 0, libLoad = 0, inasisLoad = 0, mallLoad = 0, otherLoad = 0;
        if (scenario.equals("Normal Semester")) {
            dkgLoad = peakMW * 0.42; libLoad = peakMW * 0.25; mallLoad = peakMW * 0.15; otherLoad = peakMW * 0.18;
        } else if (scenario.contains("Library Day Peak")) {
            libLoad = peakMW * 0.45; dkgLoad = peakMW * 0.22; mallLoad = peakMW * 0.15; otherLoad = peakMW * 0.18;
        } else if (scenario.contains("Inasis Night Peak")) {
            inasisLoad = peakMW * 0.58; otherLoad = peakMW * 0.42;
        }


        logArea.appendText("\n[DIAGNOSTIC] Peak Load Composition (" + String.format("%.2f", peakMW) + " MW):");
        if(dkgLoad > 0) logArea.appendText(String.format("\n  > DKG & Academic Blocks: %.2f MW", dkgLoad));
        if(libLoad > 0) logArea.appendText(String.format("\n  > Sultanah Bahiyah Library: %.2f MW", libLoad));
        if(inasisLoad > 0) logArea.appendText(String.format("\n  > Inasis Hostels (Plug Load): %.2f MW", inasisLoad));
        if(mallLoad > 0) logArea.appendText(String.format("\n  > Varsity Mall & Cafeteria: %.2f MW", mallLoad));
        logArea.appendText(String.format("\n  > Infrastructure & Pumps: %.2f MW", otherLoad));


        diagnosticHtmlTable = "<h3>Pre-Optimization Load Breakdown</h3>" +
                "<p>Root cause analysis of the " + String.format("%.2f", peakMW) + " MW overload limit breach:</p>" +
                "<table><tr><th>Asset Zone</th><th>Current Load (MW)</th><th>Diagnostic Status</th></tr>";
        if(dkgLoad > 0) diagnosticHtmlTable += "<tr><td>DKG & Academic Blocks</td><td style='color:#ef4444; font-weight:bold;'>" + String.format("%.2f", dkgLoad) + " MW</td><td>" + (dkgLoad > 3.0 ? "CRITICAL" : "HIGH") + "</td></tr>";
        if(libLoad > 0) diagnosticHtmlTable += "<tr><td>Sultanah Bahiyah Library</td><td style='color:#ef4444; font-weight:bold;'>" + String.format("%.2f", libLoad) + " MW</td><td>" + (libLoad > 3.0 ? "CRITICAL" : "HIGH") + "</td></tr>";
        if(inasisLoad > 0) diagnosticHtmlTable += "<tr><td>Inasis Hostels</td><td style='color:#ef4444; font-weight:bold;'>" + String.format("%.2f", inasisLoad) + " MW</td><td>" + (inasisLoad > 4.0 ? "CRITICAL" : "HIGH") + "</td></tr>";
        if(mallLoad > 0) diagnosticHtmlTable += "<tr><td>Varsity Mall & Cafeteria</td><td style='color:#f59e0b; font-weight:bold;'>" + String.format("%.2f", mallLoad) + " MW</td><td>MODERATE</td></tr>";
        diagnosticHtmlTable += "<tr><td>Infrastructure & Others</td><td style='color:#f59e0b; font-weight:bold;'>" + String.format("%.2f", otherLoad) + " MW</td><td>MODERATE</td></tr>";
        diagnosticHtmlTable += "<tr style='background-color:#e2e8f0; font-weight:bold;'><td>TOTAL PEAK LOAD</td><td style='color:#ef4444;'>" + String.format("%.2f", peakMW) + " MW</td><td>OVER LIMIT (>7.5 MW)</td></tr></table><br>";


        logArea.appendText("\n[AI_ENGINE] Formulating counter-measures to avoid TNB RM97.06/kW Penalty...");
        double tnbRate = 97.06;


        // IDEA 1: BESS Microgrid Feature
        if (!bessDeployed) {
            actionPanelContainer.getChildren().add(createActionNode(
                    "⚡ Deploy BESS Microgrid",
                    "Discharge Battery Storage during Peak Hours (2PM-10PM) to offset TNB Max Demand grid load.",
                    1.5,
                    (1.5 * 1000 * tnbRate),
                    99, true
            ));
        }


        // IDEA 2: Predictive Maintenance Feature
        if (anomalyDetected) {
            actionPanelContainer.getChildren().add(createActionNode(
                    "🛠️ Predictive Maintenance Ticket",
                    "Anomaly: Library Chiller Compressor efficiency dropped by 34%. High risk of failure in 7 days.",
                    0.0,
                    15000.0,
                    99, false
            ));
        }


        if (scenario.equals("Normal Semester")) {
            actionPanelContainer.getChildren().addAll(
                    createActionNode("DKG HVAC & Lighting", "Reduce AC in active halls. Disable empty halls.", 1.1, (1.1 * 1000 * tnbRate), 95, false),
                    createActionNode("Library & Office Chillers", "Throttle PSB Library & School Office AC to 80%.", 0.9, (0.9 * 1000 * tnbRate), 92, false)
            );
        } else if (scenario.contains("Library Day Peak")) {
            actionPanelContainer.getChildren().addAll(
                    createActionNode("Library Cooling Prioritized", "Maintain Library AC. Throttle School Office AC.", 1.2, (1.2 * 1000 * tnbRate), 96, false),
                    createActionNode("DKG Smart Energy", "Turn off all lights & smartboards in empty DKG halls.", 0.5, (0.5 * 1000 * tnbRate), 91, false)
            );
        } else if (scenario.contains("Inasis Night Peak")) {
            actionPanelContainer.getChildren().addAll(
                    createActionNode("Inasis HVAC Limit", "Restrict Hostel AC minimum temp to 24°C.", 0.9, (0.9 * 1000 * tnbRate), 97, false),
                    createActionNode("Campus Infrastructure", "Dim streetlights by 30% & delay water pumps.", 0.6, (0.6 * 1000 * tnbRate), 93, false)
            );
        }
    }


    private VBox createActionNode(String title, String desc, double MW_Drop, double rmValue, int aiConfidence, boolean isBess) {
        VBox box = new VBox(10);


        if (isBess) box.setStyle("-fx-background-color: #422006; -fx-padding: 20; -fx-background-radius: 10; -fx-border-color: #facc15; -fx-border-radius: 10; -fx-border-width: 2px;");
        else box.setStyle("-fx-background-color: #0f172a; -fx-padding: 20; -fx-background-radius: 10; -fx-border-color: #334155; -fx-border-radius: 10;");


        HBox header = new HBox(); header.setAlignment(Pos.CENTER_LEFT);
        Label tLabel = new Label(title);
        tLabel.setStyle(isBess ? "-fx-text-fill: #facc15; -fx-font-weight: 900;" : "-fx-text-fill: #f8fafc; -fx-font-weight: 900;");


        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label confLabel = new Label("AI Match: " + aiConfidence + "%");
        confLabel.setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
        header.getChildren().addAll(tLabel, spacer, confLabel);


        Label dLabel = new Label(desc + "\nEst. Avoided Cost: " + String.format("RM %,.2f", rmValue) + " | Load Drop: -" + MW_Drop + " MW");
        dLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");


        Button execBtn = new Button(isBess ? "⚡ DEPLOY RESERVES" : "▶ EXECUTE PROTOCOL");
        execBtn.setMaxWidth(Double.MAX_VALUE);
        execBtn.setStyle(isBess ? "-fx-background-color: #facc15; -fx-text-fill: #020617; -fx-font-weight: 900; -fx-cursor: hand;" : "-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: 900; -fx-cursor: hand;");


        execBtn.setOnAction(e -> {
            execBtn.setDisable(true); execBtn.setText("⚙️ PROCESSING...");
            execBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: 900;");


            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1.5), ev -> {
                        if (isBess) {
                            bessDeployed = true;
                            kpiBatteryLabel.setText("42% [DISCHARGING]");
                            kpiBatteryLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 20px; -fx-font-weight: 900; -fx-font-family: 'Consolas';");
                            logArea.appendText("\n[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] [BESS] Battery discharged to offset grid load.");
                        } else {
                            activeReductionsMW += MW_Drop;
                        }


                        updateGraphAlgorithm();
                        totalRmSaved += rmValue;
                        rmSavedLabel.setText(String.format("RM %,.2f", totalRmSaved));
                        logArea.appendText("\n[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] [SUCCESS] Executed: " + title);
                        execBtn.setText("✔ PROTOCOL ACTIVE");
                        execBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: 900;");


                        executedProtocolsHtml += "<tr><td>" + title + "</td><td style='color: #10b981; font-weight: bold;'>-" + String.format("%.2f", MW_Drop) + " MW</td><td style='color: #10b981; font-weight: bold;'>EXECUTED</td></tr>";
                    })
            ); timeline.play();
        });
        box.getChildren().addAll(header, dLabel, execBtn); return box;
    }


    // ================= 2. IOT BUILDING ASSETS =================
    private VBox createAssetsView() {
        VBox view = new VBox(25); view.setPadding(new Insets(40, 50, 40, 50));
        Label title = new Label("Digital Twin: Real-Time Asset Telemetry"); title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: 900;");


        GridPane grid = new GridPane(); grid.setHgap(30); grid.setVgap(30);
        assetBars.clear(); assetLoadLabels.clear();


        grid.add(createAssetCard("DKG Blocks 1-6", 0.65), 0, 0);
        grid.add(createAssetCard("Sultanah Bahiyah Library", 0.82), 1, 0);
        grid.add(createAssetCard("Hostel Inasis", 0.40), 2, 0);
        grid.add(createAssetCard("School Office & Chancellery", 0.55), 0, 1);
        grid.add(createAssetCard("UUMIT Data Center", 0.88), 1, 1);
        grid.add(createAssetCard("Varsity Mall & Cafeteria", 0.75), 2, 1);


        view.getChildren().addAll(title, new Label("Live MQTT streams from campus Edge nodes. Manual override enabled for Facility Managers."), grid);
        ((Label)view.getChildren().get(1)).setStyle("-fx-text-fill: #94a3b8;"); return view;
    }


    private VBox createAssetCard(String name, double initialLoad) {
        VBox card = new VBox(15); card.setPadding(new Insets(25)); card.setPrefSize(380, 180);
        card.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-radius: 10;");


        HBox header = new HBox(8); header.setAlignment(Pos.CENTER_LEFT);
        Label n = new Label(name); n.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);


        Button configBtn = new Button("⚙️ CONFIG");
        configBtn.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: #020617; -fx-font-weight: bold; -fx-font-size: 10px; -fx-cursor: hand;");


        Button overrideBtn = new Button("OVERRIDE");
        overrideBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-cursor: hand;");


        header.getChildren().addAll(n, spacer, configBtn, overrideBtn);


        Label l = new Label(String.format("Current Load: %.2f MW", initialLoad * 5)); l.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: 'Consolas';");
        ProgressBar pb = new ProgressBar(initialLoad); pb.setPrefWidth(330); pb.setPrefHeight(15);
        assetLoadLabels.add(l); assetBars.add(pb);


        // IDEA 2: Health Status Indicator
        Label healthLbl = new Label("⚙️ Health: 98% [OPTIMAL]");
        healthLbl.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: bold;");


        // Save the Library's health label to our global variable for the demo
        if (name.contains("Library")) {
            demoHealthLabel = healthLbl;
        }


        configBtn.setOnAction(e -> openTelemetryModal(name, pb, l, initialLoad));


        overrideBtn.setOnAction(e -> {
            if(overrideBtn.getText().equals("OVERRIDE")) {
                pb.setProgress(0.05);
                l.setText("Current Load: 0.25 MW (MANUAL OVERRIDE)");
                l.setStyle("-fx-text-fill: #ef4444; -fx-font-family: 'Consolas';");
                overrideBtn.setText("RESTORE");
                overrideBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
            } else {
                pb.setProgress(initialLoad);
                l.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: 'Consolas';");
                overrideBtn.setText("OVERRIDE");
                overrideBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
            }
        });


        card.getChildren().addAll(header, pb, l, healthLbl); return card;
    }


    private void openTelemetryModal(String buildingName, ProgressBar pb, Label loadLabel, double baseLoad) {
        Stage modal = new Stage();
        modal.setTitle("IoT Edge Node - " + buildingName);


        VBox layout = new VBox(25);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #020617; -fx-border-color: #38bdf8; -fx-border-width: 2px;");


        Label title = new Label(buildingName + "\nSub-System SCADA Control");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 900;");


        layout.getChildren().addAll(title, new Separator(),
                createSubsystemRow("❄️ Central HVAC Chiller", 0.60, pb, loadLabel, baseLoad),
                createSubsystemRow("💡 Smart Lighting Network", 0.25, pb, loadLabel, baseLoad),
                createSubsystemRow("🔌 Plug Loads & Servers", 0.15, pb, loadLabel, baseLoad)
        );


        Button closeBtn = new Button("CLOSE TERMINAL");
        closeBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> modal.close());


        layout.getChildren().addAll(new Separator(), closeBtn);
        Scene scene = new Scene(layout, 450, 420);
        modal.setScene(scene); modal.setResizable(false); modal.show();
    }


    private HBox createSubsystemRow(String sysName, double weightPercentage, ProgressBar pb, Label loadLabel, double baseLoad) {
        HBox row = new HBox(15); row.setAlignment(Pos.CENTER_LEFT);
        VBox texts = new VBox(5);
        Label n = new Label(sysName); n.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label s = new Label("Status: AUTO (AI Managed)"); s.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");
        texts.getChildren().addAll(n, s);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button toggleBtn = new Button("DISABLE");
        toggleBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");


        toggleBtn.setOnAction(e -> {
            double currentP = pb.getProgress();
            double loadDropAmount = baseLoad * weightPercentage;
            if(toggleBtn.getText().equals("DISABLE")) {
                toggleBtn.setText("ENABLE"); toggleBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                s.setText("Status: MANUAL OVERRIDE (OFF)"); s.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                pb.setProgress(Math.max(0.05, currentP - loadDropAmount));
                loadLabel.setText(String.format("Current Load: %.2f MW (Sub-System OFF)", pb.getProgress() * 5));
            } else {
                toggleBtn.setText("DISABLE"); toggleBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                s.setText("Status: AUTO (AI Managed)"); s.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");
                pb.setProgress(Math.min(1.0, currentP + loadDropAmount));
                loadLabel.setText(String.format("Current Load: %.2f MW", pb.getProgress() * 5));
            }
        });
        row.getChildren().addAll(texts, spacer, toggleBtn); return row;
    }


    private void simulateAssetFluctuations() {
        for (int i = 0; i < assetBars.size(); i++) {
            ProgressBar pb = assetBars.get(i);
            if (pb.getProgress() > 0.1) {
                double newLoad = pb.getProgress() + ((random.nextDouble() * 0.04) - 0.02);
                if(newLoad > 0.1 && newLoad < 1) {
                    pb.setProgress(newLoad);
                    assetLoadLabels.get(i).setText(String.format("Current Load: %.2f MW", newLoad * 5));
                }
            }
        }
    }


    // ================= 3. FINANCIAL ROI =================
    private VBox createROIView() {
        VBox view = new VBox(20); view.setPadding(new Insets(40, 50, 40, 50));
        Label title = new Label("TNB ToU Monthly Bill Projection (4-Step Breakdown)");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: 900;");


        double dailyPeakMWh = 0, dailyOffPeakMWh = 0, dailyBasePeakMWh = 0, dailyBaseOffPeakMWh = 0;
        double optMaxPeakMW = 0, baseMaxPeakMW = 0;


        for (int i = 0; i <= 24; i++) {
            double optLoad = demandSeries.getData().isEmpty() ? 2.0 : demandSeries.getData().get(i).getYValue().doubleValue();
            double baseLoad = baselineSeries.getData().isEmpty() ? 2.0 : baselineSeries.getData().get(i).getYValue().doubleValue();


            if (i >= 14 && i <= 22) {
                dailyPeakMWh += optLoad; dailyBasePeakMWh += baseLoad;
                if (optLoad > optMaxPeakMW) optMaxPeakMW = optLoad;
                if (baseLoad > baseMaxPeakMW) baseMaxPeakMW = baseLoad;
            } else {
                dailyOffPeakMWh += optLoad; dailyBaseOffPeakMWh += baseLoad;
            }
        }


        double optMonthlyPeakKwh = (dailyPeakMWh * 22) * 1000;
        double optMonthlyOffPeakKwh = ((dailyOffPeakMWh * 22) + ((dailyPeakMWh + dailyOffPeakMWh) * 8)) * 1000;
        double optMaxDemandKw = optMaxPeakMW * 1000;


        double optS1 = optMonthlyPeakKwh * 0.3132;
        double optS2 = optMonthlyOffPeakKwh * 0.2723;
        double optS3 = optMaxDemandKw * 30.19;
        double optS4 = optMaxDemandKw * 66.87;
        double optTotalBill = optS1 + optS2 + optS3 + optS4;


        double baseMonthlyPeakKwh = (dailyBasePeakMWh * 22) * 1000;
        double baseMonthlyOffPeakKwh = ((dailyBaseOffPeakMWh * 22) + ((dailyBasePeakMWh + dailyBaseOffPeakMWh) * 8)) * 1000;
        double baseMaxDemandKw = baseMaxPeakMW * 1000;
        double baseTotalBill = (baseMonthlyPeakKwh * 0.3132) + (baseMonthlyOffPeakKwh * 0.2723) + (baseMaxDemandKw * 30.19) + (baseMaxDemandKw * 66.87);


        HBox contentBox = new HBox(40);
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Monthly Bill (RM)");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setPrefWidth(350); barChart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Without AI", baseTotalBill));
        series.getData().add(new XYChart.Data<>("With AI", optTotalBill));
        barChart.getData().add(series);


        Platform.runLater(() -> {
            if(!series.getData().isEmpty()){
                series.getData().get(0).getNode().setStyle("-fx-bar-fill: #ef4444;");
                series.getData().get(1).getNode().setStyle("-fx-bar-fill: #10b981;");
            }
        });


        VBox breakdownBox = new VBox(15);
        breakdownBox.setPadding(new Insets(25));
        breakdownBox.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-radius: 10;");
        breakdownBox.setPrefWidth(550);


        Label bTitle = new Label("LIVE TNB CALCULATION (OPTIMIZED BILL)");
        bTitle.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 14px;");


        breakdownBox.getChildren().addAll(
                bTitle, new Separator(),
                createBreakdownRow("Step 1: Peak Energy Cost", String.format("%,.0f kWh × RM 0.3132", optMonthlyPeakKwh), optS1, "#f8fafc"),
                createBreakdownRow("Step 2: Off-Peak Energy Cost", String.format("%,.0f kWh × RM 0.2723", optMonthlyOffPeakKwh), optS2, "#f8fafc"),
                createBreakdownRow("Step 3: Capacity Charge (Max Demand)", String.format("%,.0f kW × RM 30.19", optMaxDemandKw), optS3, "#f59e0b"),
                createBreakdownRow("Step 4: Network Charge (Max Demand)", String.format("%,.0f kW × RM 66.87", optMaxDemandKw), optS4, "#f59e0b"),
                new Separator(),
                createBreakdownRow("FINAL ESTIMATED MONTHLY BILL", "Before Gov Taxes & Fuel Adjustments", optTotalBill, "#10b981")
        );


        contentBox.getChildren().addAll(barChart, breakdownBox);


        Button exportBtn = new Button("📄 EXPORT ESG COMPLIANCE REPORT (PDF)");
        exportBtn.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> {
            exportBtn.setText("⏳ GENERATING REPORT..."); exportBtn.setDisable(true);
            PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
            pt.setOnFinished(ev -> {
                generateLiveESGReport();
                exportBtn.setText("📄 EXPORT ESG COMPLIANCE REPORT (PDF)"); exportBtn.setDisable(false);
            }); pt.play();
        });


        view.getChildren().addAll(title, contentBox, exportBtn); return view;
    }


    private VBox createBreakdownRow(String stepTitle, String formula, double amount, String colorHex) {
        VBox box = new VBox(2);
        HBox row = new HBox(); row.setAlignment(Pos.CENTER_LEFT);
        Label t1 = new Label(stepTitle); t1.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label t2 = new Label(String.format("RM %,.2f", amount));
        t2.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: 900; -fx-font-size: 14px;");
        row.getChildren().addAll(t1, spacer, t2);
        Label fLbl = new Label(formula); fLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        box.getChildren().addAll(row, fLbl);
        return box;
    }


    private void generateLiveESGReport() {
        try {
            File reportFile = new File("SmartCampus_ESG_Report.html");
            FileWriter writer = new FileWriter(reportFile);


            String dateStr = new SimpleDateFormat("dd MMMM yyyy, HH:mm:ss").format(new Date());
            String scenario = scenarioBox != null ? scenarioBox.getValue() : "Normal";


            double dailyPeakMWh = 0, dailyOffPeakMWh = 0;
            double optMaxPeakMW = 0;


            for (int i = 0; i <= 24; i++) {
                double optLoad = demandSeries.getData().isEmpty() ? 2.0 : demandSeries.getData().get(i).getYValue().doubleValue();
                if (i >= 14 && i <= 22) {
                    dailyPeakMWh += optLoad;
                    if (optLoad > optMaxPeakMW) optMaxPeakMW = optLoad;
                } else {
                    dailyOffPeakMWh += optLoad;
                }
            }
            double optMonthlyPeakKwh = (dailyPeakMWh * 22) * 1000;
            double optMonthlyOffPeakKwh = ((dailyOffPeakMWh * 22) + ((dailyPeakMWh + dailyOffPeakMWh) * 8)) * 1000;
            double optMaxDemandKw = optMaxPeakMW * 1000;


            double optS1 = optMonthlyPeakKwh * 0.3132;
            double optS2 = optMonthlyOffPeakKwh * 0.2723;
            double optS3 = optMaxDemandKw * 30.19;
            double optS4 = optMaxDemandKw * 66.87;
            double optTotalBill = optS1 + optS2 + optS3 + optS4;


            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>ESG Report</title>" +
                    "<style>" +
                    "body { font-family: 'Segoe UI', Arial, sans-serif; color: #1e293b; padding: 40px; background-color: #f8fafc; }" +
                    ".container { max-width: 800px; margin: auto; background: white; padding: 40px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
                    ".header { border-bottom: 3px solid #10b981; padding-bottom: 20px; margin-bottom: 30px; text-align: center; }" +
                    ".title { font-size: 28px; font-weight: 900; color: #0f172a; margin: 0; }" +
                    ".subtitle { color: #64748b; font-size: 14px; }" +
                    ".kpi-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px; }" +
                    ".kpi-box { background: #f1f5f9; padding: 20px; border-radius: 8px; border-left: 5px solid #38bdf8; }" +
                    ".kpi-title { font-size: 12px; font-weight: bold; color: #64748b; text-transform: uppercase; }" +
                    ".kpi-value { font-size: 32px; font-weight: 900; color: #10b981; margin-top: 5px; }" +
                    "table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }" +
                    "th, td { padding: 15px; border-bottom: 1px solid #e2e8f0; text-align: left; }" +
                    "th { background-color: #0f172a; color: white; font-weight: bold; }" +
                    "tr:nth-child(even) { background-color: #f8fafc; }" +
                    ".footer { margin-top: 50px; text-align: center; font-size: 12px; color: #94a3b8; line-height: 1.5; }" +
                    "</style>" +
                    "</head><body onload='window.print()'>" +


                    "<div class='container'>" +
                    "<div class='header'>" +
                    "<h1 class='title'>⚡ SmartCampus TNB ToU Optimization Report</h1>" +
                    "<p class='subtitle'>Generated for Universiti Utara Malaysia (UUM) Facility Management</p>" +
                    "<p class='subtitle'>Date: " + dateStr + " | Scenario: " + scenario + "</p>" +
                    "</div>" +


                    "<div class='kpi-grid'>" +
                    "<div class='kpi-box'>" +
                    "<div class='kpi-title'>Maximum Demand Avoided Cost</div>" +
                    "<div class='kpi-value'>RM " + String.format("%,.2f", totalRmSaved) + "</div>" +
                    "</div>" +
                    "<div class='kpi-box' style='border-left-color: #ef4444;'>" +
                    "<div class='kpi-title'>Peak Load Reduced</div>" +
                    "<div class='kpi-value'>-" + String.format("%.2f", activeReductionsMW + (bessDeployed ? 1.5 : 0)) + " MW</div>" +
                    "</div>" +
                    "</div>" +


                    (diagnosticHtmlTable != null ? diagnosticHtmlTable : "") +


                    "<h3>TNB ToU 4-Step Monthly Bill Projection</h3>" +
                    "<table>" +
                    "<tr><th>Calculation Step</th><th>Formula Applied</th><th>Cost (RM)</th></tr>" +
                    "<tr><td>Step 1: Peak Energy Cost</td><td>" + String.format("%,.0f kWh × RM 0.3132", optMonthlyPeakKwh) + "</td><td>RM " + String.format("%,.2f", optS1) + "</td></tr>" +
                    "<tr><td>Step 2: Off-Peak Energy Cost</td><td>" + String.format("%,.0f kWh × RM 0.2723", optMonthlyOffPeakKwh) + "</td><td>RM " + String.format("%,.2f", optS2) + "</td></tr>" +
                    "<tr><td>Step 3: Capacity Charge</td><td>" + String.format("%,.0f kW × RM 30.19", optMaxDemandKw) + "</td><td>RM " + String.format("%,.2f", optS3) + "</td></tr>" +
                    "<tr><td>Step 4: Network Charge</td><td>" + String.format("%,.0f kW × RM 66.87", optMaxDemandKw) + "</td><td>RM " + String.format("%,.2f", optS4) + "</td></tr>" +
                    "<tr style='background-color:#e2e8f0; font-weight:bold;'><td>Total Optimized Monthly Bill</td><td>Before Tax</td><td style='color:#10b981;'>RM " + String.format("%,.2f", optTotalBill) + "</td></tr>" +
                    "</table><br>" +


                    "<h3>Active Reduction Breakdown</h3>" +
                    "<p>The following assets were autonomously optimized by the AI Engine to prevent grid overload:</p>" +
                    "<table>" +
                    "<tr><th>Targeted Asset / Action</th><th>Energy Dropped (MW)</th><th>Status</th></tr>";


            if ((activeReductionsMW > 0 || bessDeployed || anomalyDetected) && !executedProtocolsHtml.isEmpty()) {
                html += executedProtocolsHtml;
            } else {
                html += "<tr><td colspan='3' style='text-align:center; color:#64748b;'>No active interventions currently required. Grid is stable.</td></tr>";
            }


            html += "</table>" +
                    "<div class='footer'>This document is generated dynamically by the SmartCampus AI Engine V13.0.<br>Validated against TNB Non-Domestic Medium Voltage ToU Tariff (Effective July 2025). Peak Hours: 2PM-10PM.</div>" +
                    "</div></body></html>";


            writer.write(html);
            writer.close();
            getHostServices().showDocument(reportFile.toURI().toString());


        } catch (Exception e) {
            System.out.println("Error generating report: " + e.getMessage());
        }
    }


    // ================= 4. SYSTEM SETTINGS =================
    private VBox createSettingsView() {
        VBox view = new VBox(25);
        view.setPadding(new Insets(40, 50, 40, 50));


        Label title = new Label("SaaS AI Parameter Tuning & Server Status");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: 900;");
        Label sub = new Label("Advanced administrative console. Changes applied here immediately affect global AI algorithm behavior.");
        sub.setStyle("-fx-text-fill: #94a3b8;");


        HBox mainContent = new HBox(40);


        VBox leftCol = new VBox(30);
        leftCol.setPrefWidth(400);
        leftCol.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-radius: 8; -fx-padding: 30;");
        Label lTitle = new Label("⚙️ AI CORE PARAMETERS");
        lTitle.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 14px;");


        VBox tBox = new VBox(15);
        Label tLbl = new Label("Grid Penalty Threshold (MW)"); tLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        Slider tSlider = new Slider(5.0, 10.0, currentThreshold);
        tSlider.setShowTickLabels(true); tSlider.setShowTickMarks(true);
        tSlider.setStyle("-fx-control-inner-background: #1e293b;");
        Label tVal = new Label(String.format("Current Setpoint: %.1f MW", currentThreshold));
        tVal.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-family: 'Consolas';");
        tSlider.valueProperty().addListener((obs, old, newV) -> {
            currentThreshold = newV.doubleValue();
            tVal.setText(String.format("Current Setpoint: %.1f MW", currentThreshold));
            resetScenarioState();
        });
        tBox.getChildren().addAll(tLbl, tSlider, tVal);


        VBox cBox = new VBox(10);
        Label cLbl = new Label("TNB ToU Max Demand Rate (RM/kW)"); cLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        TextField cField = new TextField("97.06"); cField.setMaxWidth(150);
        cField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #10b981; -fx-border-color: #38bdf8; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
        Label subLbl = new Label("Formula: Capacity RM30.19 + Network RM66.87"); subLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        cBox.getChildren().addAll(cLbl, cField, subLbl);


        leftCol.getChildren().addAll(lTitle, new Separator(), tBox, cBox);


        VBox rightCol = new VBox(25);
        rightCol.setPrefWidth(450);


        VBox serverBox = new VBox(15);
        serverBox.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-radius: 8; -fx-padding: 25;");
        Label sTitle = new Label("☁️ CLOUD INFRASTRUCTURE UPLINK"); sTitle.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 14px;");
        Label l1 = new Label("● MQTT IoT Broker: CONNECTED [12ms ping]"); l1.setStyle("-fx-text-fill: #10b981; -fx-font-family: 'Consolas';");
        Label l2 = new Label("● XGBoost AI Core: ONLINE & SYNCED"); l2.setStyle("-fx-text-fill: #10b981; -fx-font-family: 'Consolas';");
        Label l3 = new Label("● UUM Mainframe API: AUTHENTICATED"); l3.setStyle("-fx-text-fill: #10b981; -fx-font-family: 'Consolas';");
        serverBox.getChildren().addAll(sTitle, new Separator(), l1, l2, l3);


        VBox diagBox = new VBox(10);
        Label dTitle = new Label("NETWORK DIAGNOSTICS TOOL"); dTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 11px;");
        TextArea diagLog = new TextArea();
        diagLog.setEditable(false); diagLog.setPrefHeight(130);
        diagLog.setStyle("-fx-control-inner-background: #020617; -fx-text-fill: #34d399; -fx-font-family: 'Consolas'; -fx-border-color: #1e293b;");
        diagLog.setText("Waiting for admin command...\n");


        Button pingBtn = new Button("RUN SYSTEM PING SWEEP");
        pingBtn.setMaxWidth(Double.MAX_VALUE);
        pingBtn.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: #0f172a; -fx-font-weight: 900; -fx-cursor: hand; -fx-padding: 10;");
        pingBtn.setOnAction(e -> {
            pingBtn.setDisable(true);
            pingBtn.setText("SCANNING NETWORK...");
            diagLog.setText("Initiating ping sweep to campus Edge nodes...\n");


            Timeline tl = new Timeline(
                    new KeyFrame(Duration.seconds(0.5), ev -> diagLog.appendText("[OK] DKG MQTT Gateway ... 14ms\n")),
                    new KeyFrame(Duration.seconds(1.0), ev -> diagLog.appendText("[OK] Library Chiller Node ... 18ms\n")),
                    new KeyFrame(Duration.seconds(1.5), ev -> diagLog.appendText("[OK] Inasis Smart Meter ... 22ms\n")),
                    new KeyFrame(Duration.seconds(2.0), ev -> diagLog.appendText("[OK] BESS Storage Controller ... 10ms\n")),
                    new KeyFrame(Duration.seconds(2.5), ev -> {
                        diagLog.appendText("\n[SUCCESS] All hardware nodes responsive.\n");
                        pingBtn.setText("RUN SYSTEM PING SWEEP");
                        pingBtn.setDisable(false);
                    })
            );
            tl.play();
        });


        // IDEA 2: Secret Demo Trigger Button
        Button anomalyBtn = new Button("⚠ SIMULATE HARDWARE ANOMALY");
        anomalyBtn.setMaxWidth(Double.MAX_VALUE);
        anomalyBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 900; -fx-cursor: hand; -fx-padding: 10;");


        anomalyBtn.setOnAction(e -> {
            anomalyDetected = true;
            diagLog.appendText("\n[WARNING] Telemetry anomaly detected at Library Chiller!\n");


            // Change the UI label on the Assets page to red
            if (demoHealthLabel != null) {
                demoHealthLabel.setText("⚙️ Health: 62% [COMPRESSOR DEGRADATION]");
                demoHealthLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: bold;");
            }


            // Force the AI to recalculate and show the new protocol
            updateGraphAlgorithm();
        });


        diagBox.getChildren().addAll(dTitle, diagLog, pingBtn, anomalyBtn);
        rightCol.getChildren().addAll(serverBox, diagBox);


        mainContent.getChildren().addAll(leftCol, rightCol);
        view.getChildren().addAll(title, sub, mainContent);
        return view;
    }


    private Button createNavButton(String text) {
        Button btn = new Button(text); btn.setPrefWidth(210); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 15 20; -fx-font-weight: bold;");
        navButtons.add(btn); return btn;
    }


    private void setActiveNav(Button activeBtn) {
        for (Button btn : navButtons) btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 15 20; -fx-font-weight: bold;");
        activeBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-padding: 15 20; -fx-font-weight: bold; -fx-border-color: #38bdf8; -fx-border-width: 0 0 0 4px;");
    }


    public static void main(String[] args) { launch(args); }
}
