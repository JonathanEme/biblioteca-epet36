package com.epet36.biblioteca.ui;

import com.epet36.biblioteca.BibliotecaApplication;
import com.epet36.biblioteca.event.StageReadyEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        // Inicializa Spring Boot pasando los argumentos de la línea de comandos
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.applicationContext = new SpringApplicationBuilder()
                .sources(BibliotecaApplication.class)
                .run(args);
    }

    @Override
    public void start(Stage stage) {
        // Notifica a Spring que JavaFX está listo y entrega el Stage principal
        applicationContext.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        // Cierra correctamente el contexto de Spring al cerrar la app
        this.applicationContext.close();
        Platform.exit();
    }
}