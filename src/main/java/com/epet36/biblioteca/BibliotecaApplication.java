package com.epet36.biblioteca;

import com.epet36.biblioteca.ui.JavaFxApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BibliotecaApplication {

	public static void main(String[] args) {
		// Inicializamos la aplicación a través de JavaFX.
		// JavaFxApplication se encargará de levantar Spring Boot por detrás.
		Application.launch(JavaFxApplication.class, args);
	}
	{}
}