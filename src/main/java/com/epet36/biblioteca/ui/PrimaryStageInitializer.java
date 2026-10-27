package com.epet36.biblioteca.ui;

import com.epet36.biblioteca.event.StageReadyEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    private final ApplicationContext applicationContext;
    private final Resource loginFxml;
    private final String applicationTitle;

    public PrimaryStageInitializer(
            ApplicationContext applicationContext,
            @Value("classpath:/view/login.fxml") Resource loginFxml, // APUNTA AL LOGIN
            @Value("${spring.application.name:Sistema de Gestión de Biblioteca}") String applicationTitle) {
        this.applicationContext = applicationContext;
        this.loginFxml = loginFxml;
        this.applicationTitle = applicationTitle;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(loginFxml.getURL());
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();

            Stage stage = event.getStage();
            stage.setScene(new Scene(root, 400, 500)); // Tamaño chico para el login
            stage.setTitle(applicationTitle);
            stage.centerOnScreen();
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            throw new RuntimeException("Error fatal al cargar login.fxml", e);
        }
    }
}