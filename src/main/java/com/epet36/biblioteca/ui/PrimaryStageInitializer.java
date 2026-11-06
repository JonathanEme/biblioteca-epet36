package com.epet36.biblioteca.ui;

import com.epet36.biblioteca.event.StageReadyEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
    private final Resource iconResource; // <-- Inyectamos el icono como recurso de Spring
    private final String applicationTitle;

    public PrimaryStageInitializer(
            ApplicationContext applicationContext,
            @Value("classpath:/view/login.fxml") Resource loginFxml,
            @Value("classpath:/images/36.jfif") Resource iconResource, // <-- Apunta a src/main/resources/images/36.ico
            @Value("${spring.application.name:Sistema de Gestión de Biblioteca}") String applicationTitle) {
        this.applicationContext = applicationContext;
        this.loginFxml = loginFxml;
        this.iconResource = iconResource;
        this.applicationTitle = applicationTitle;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(loginFxml.getURL());
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();

            Stage stage = event.getStage();
            stage.setScene(new Scene(root, 400, 500));
            stage.setTitle(applicationTitle);
            stage.centerOnScreen();
            stage.setResizable(false);

            // ========================================================
            // CARGA SEGURA DEL ICONO INSTITUCIONAL MEDIANTE SPRING RESOURCE
            // ========================================================
            try {
                if (iconResource.exists()) {
                    stage.getIcons().add(new Image(iconResource.getInputStream()));
                } else {
                    System.err.println("El archivo de icono no se encontró en la ruta especificada.");
                }
            } catch (Exception e) {
                System.err.println("No se pudo cargar el icono institucional: " + e.getMessage());
            }

            stage.show();

        } catch (IOException e) {
            throw new RuntimeException("Error fatal al cargar login.fxml", e);
        }
    }
}