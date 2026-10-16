package com.van_uber.famapos.controller;

import com.van_uber.famapos.domain.Operator;
import com.van_uber.famapos.repository.OperatorRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@FxmlView("session_settings_modal.fxml")
public class SessionSettingsModalController {

    private final OperatorRepository operatorRepository;
    private final FxWeaver fxWeaver;

    @FXML private TextField userField;
    @FXML private PasswordField pwdField;
    @FXML private PasswordField pwdConfirmField;
    @FXML private PasswordField currentPwdField;

    @Autowired
    public SessionSettingsModalController(OperatorRepository operatorRepository, FxWeaver fxWeaver) {
        this.operatorRepository = operatorRepository;
        this.fxWeaver = fxWeaver;
    }

    @FXML
    public void initialize() {
        // Load the only operator available
        List<Operator> operators = operatorRepository.findAll();
        if (!operators.isEmpty()) {
            userField.setText(operators.get(0).getUsername());
            pwdField.setText(operators.get(0).getPassword());
        }
    }

    @FXML
    public void onSaveCredentials(ActionEvent event) {
        String newUsername = userField.getText().trim();
        String newPwd = pwdField.getText().trim();
        String pwdConfirm = pwdConfirmField.getText().trim();
        String currentPwd = currentPwdField.getText().trim();

        if (newUsername.isEmpty() || newPwd.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campos Vacíos", "El usuario y la nueva contraseña no pueden estar vacíos.");
            return;
        }
        
        if (!newPwd.equals(pwdConfirm)) {
            showAlert(Alert.AlertType.WARNING, "Error de Confirmación", "Las contraseñas nuevas no coinciden.");
            return;
        }

        List<Operator> operators = operatorRepository.findAll();
        Operator op;
        if (!operators.isEmpty()) {
            op = operators.get(0);
            if (!op.getPassword().equals(currentPwd)) {
                showAlert(Alert.AlertType.ERROR, "Autorización Denegada", "La contraseña actual es incorrecta.");
                return;
            }
        } else {
            op = new Operator();
        }
        
        op.setUsername(newUsername);
        op.setPassword(newPwd);
        operatorRepository.save(op);
        
        showAlert(Alert.AlertType.INFORMATION, "Guardado", "Credenciales actualizadas exitosamente.\nEntrarán en vigor la próxima vez y aplican para los permisos de administrador (Eliminar tickets, borrar productos, etc).");
        closeWindow(event);
    }

    @FXML
    public void onLogout(ActionEvent event) {
        closeWindow(event);
        // We close the modal, and we need to navigate the MAIN stage backwards to login.
        // We can find the main window by getting any active stage or using the FxWeaver root context if easily available.
        // Easiest is to close *all* windows and rebuild the login stage, but JavaFX doesn't easily let us retrieve the MAIN stage from a modal directly unless passed.
        // Since the App operates on single screen, we can just iterate windows.
        
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage && ((Stage) window).getTitle() != null && ((Stage) window).getTitle().equals("FaMa POS")) {
                Stage mainStage = (Stage) window;
                Parent root = fxWeaver.loadView(LoginController.class);
                mainStage.setScene(new Scene(root, 1024, 768));
                break;
            }
        }
    }

    @FXML
    public void onExitSystem(ActionEvent event) {
        javafx.application.Platform.exit();
        System.exit(0);
    }

    @FXML
    public void onCancel(ActionEvent event) {
        closeWindow(event);
    }

    private void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
