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

@Component // ¡CRÍTICO! Si falta esto, Spring no lo detecta y la ventana nunca se abre.
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    private final ApplicationContext applicationContext;
    private final Resource mainFxml;
    private final String applicationTitle;

    // Usamos el constructor para inyectar el contexto y las propiedades
    public PrimaryStageInitializer(
            ApplicationContext applicationContext,
            // Asegúrate de que esta ruta coincida exactamente con tu archivo en resources
            @Value("classpath:/view/main.fxml") Resource mainFxml,
            @Value("${spring.application.name:Sistema de Gestión de Biblioteca}") String applicationTitle) {

        this.applicationContext = applicationContext;
        this.mainFxml = mainFxml;
        this.applicationTitle = applicationTitle;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            // 1. Preparamos el cargador del archivo FXML
            FXMLLoader fxmlLoader = new FXMLLoader(mainFxml.getURL());

            // 2. ¡LA MAGIA DE SPRING! Le decimos a JavaFX que use Spring para crear los controladores FXML
            // Esto permite que el @RequiredArgsConstructor y la inyección de servicios funcionen en NuevoPrestamoController
            fxmlLoader.setControllerFactory(applicationContext::getBean);

            // 3. Cargamos la vista
            Parent root = fxmlLoader.load();

            // 4. Configuramos y mostramos la ventana principal (Stage)
            Stage stage = event.getStage();
            stage.setScene(new Scene(root, 700, 600)); // Ajusta el tamaño si lo necesitas
            stage.setTitle(applicationTitle);
            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show(); // ¡Esto es lo que hace visible la ventana!

        } catch (IOException e) {
            // Si hay un error (ej. no encuentra el FXML), lo imprimimos para saber qué falló
            throw new RuntimeException("Error fatal al cargar la vista principal FXML: " + mainFxml.getFilename(), e);
        }
    }
}