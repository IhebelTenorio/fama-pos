package com.van_uber.famapos.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import javafx.scene.Node;
import com.van_uber.famapos.repository.OperatorRepository;
import com.van_uber.famapos.domain.Operator;
import java.util.Optional;
@Component
@FxmlView("login.fxml")
public class LoginController {

    private final FxWeaver fxWeaver;

    @FXML
    private TextField userField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final OperatorRepository operatorRepository;

    @Autowired
    public LoginController(FxWeaver fxWeaver, OperatorRepository operatorRepository) {
        this.fxWeaver = fxWeaver;
        this.operatorRepository = operatorRepository;
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = userField.getText();
        String password = passwordField.getText();

        Optional<Operator> opOpt = operatorRepository.findByUsername(username);

        if (opOpt.isPresent() && opOpt.get().getPassword().equals(password)) {
            Parent root = fxWeaver.loadView(SalesController.class);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Apply bounds from current screen so it doesnt shrink if it was maximized
            boolean wasMaximized = stage.isMaximized();
            stage.setScene(new Scene(root, 1024, 768));
            
            if (wasMaximized) {
                stage.setMaximized(true);
            } else {
                stage.centerOnScreen();
            }
            
        } else {
            errorLabel.setText("Credenciales inválidas.");
        }
    }
    
    @FXML
    public void onCloseApp(ActionEvent event) {
        javafx.application.Platform.exit();
        System.exit(0);
    }
}
