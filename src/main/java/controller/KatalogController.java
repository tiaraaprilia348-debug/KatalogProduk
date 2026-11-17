package controller;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import data.ProdukRepository;
import model.Produk;

public class KatalogController {

    @FXML
    private TextField namaField;
    @FXML
    private TextField hargaField;
    @FXML
    private ComboBox<String> fotoBox;
    @FXML
    private Label pesanLabel;
    @FXML
    private Label jumlahLabel;
    @FXML
    private TilePane katalogPane;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private final ProdukRepository repository = new ProdukRepository();
    // Nilai 0 berarti form sedang digunakan untuk menambah produk baru.
    private long editingId = 0;

    @FXML
    private void initialize() {
        fotoBox.getItems().addAll("roti.png", "susu.png", "kopi.png");
        fotoBox.setValue("roti.png");
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        try {
            showData();
        } catch (SQLException e) {
            pesanLabel.setText("Gagal membaca database. Lihat Output.");
            e.printStackTrace();
        }
    }

    private void showData() throws SQLException {
        ArrayList<Produk> daftar = repository.getAll();
        katalogPane.getChildren().clear();
        for (Produk produk : daftar) {
            katalogPane.getChildren().add(createProductCard(produk));
        }
        jumlahLabel.setText("Jumlah produk: " + daftar.size());
    }

    private VBox createProductCard(Produk produk) {
        ImageView fotoView = new ImageView();
        fotoView.setFitWidth(170);
        fotoView.setFitHeight(110);
        fotoView.setPreserveRatio(true);

        StackPane bingkai = new StackPane();
        bingkai.setPrefSize(180, 120);
        bingkai.setMinSize(180, 120);
        bingkai.setMaxSize(180, 120);

        URL lokasi = getClass().getResource("/image/" + produk.getFoto());
        if (lokasi != null) {
            Image gambar = new Image(lokasi.toExternalForm(), 170, 110, true, true);
            if (!gambar.isError()) {
                fotoView.setImage(gambar);
                bingkai.getChildren().add(fotoView);
            } else {
                bingkai.getChildren().add(new Label("Gambar tidak terbaca"));
            }
        } else {
            bingkai.getChildren().add(new Label("Gambar tidak tersedia"));
        }

        Label namaProdukLabel = new Label(produk.getNama());
        namaProdukLabel.setWrapText(true);
        namaProdukLabel.getStyleClass().add("nama-produk");
        Label hargaLabel = new Label("Rp" + produk.getHarga());
        Label idLabel = new Label("ID: " + produk.getId());
        idLabel.getStyleClass().add("muted");

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("button-edit");
        editButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setOnAction(e -> editProduct(produk));

        Button hapusButton = new Button("Hapus");
        hapusButton.getStyleClass().add("button-delete");
        hapusButton.setMaxWidth(Double.MAX_VALUE);
        hapusButton.setOnAction(e -> deleteProduct(produk));

        VBox kartu = new VBox(8);
        kartu.setMinWidth(220);
        kartu.setPrefWidth(220);
        kartu.setMaxWidth(220);
        kartu.getStyleClass().add("kartu-produk");
        kartu.getChildren().addAll(
                bingkai, namaProdukLabel, hargaLabel, idLabel,
                editButton, hapusButton);
        return kartu;
    }

    /** Memasukkan data produk terpilih ke form agar dapat diedit. */
    private void editProduct(Produk produk) {
        editingId = produk.getId();
        namaField.setText(produk.getNama());
        hargaField.setText(String.valueOf(produk.getHarga()));
        fotoBox.setValue(produk.getFoto());
        saveButton.setText("Simpan Perubahan");
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        pesanLabel.setText("Sedang mengedit produk: " + produk.getNama());
        namaField.requestFocus();
    }

    /** Meminta konfirmasi sebelum menghapus produk dari database. */
    private void deleteProduct(Produk produk) {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.setHeaderText("Hapus produk?");
        konfirmasi.setContentText("Yakin ingin menghapus produk '"
                + produk.getNama() + "'?");

        konfirmasi.showAndWait().ifPresent(jawaban -> {
            if (jawaban == ButtonType.OK) {
                try {
                    repository.delete(produk.getId());
                    if (editingId == produk.getId()) {
                        resetForm();
                    }
                    showData();
                    pesanLabel.setText("Produk berhasil dihapus.");
                } catch (SQLException e) {
                    pesanLabel.setText("Gagal menghapus produk. Lihat Output.");
                    e.printStackTrace();
                }
            }
        });
    }

    /** Mengembalikan form ke mode tambah produk. */
    @FXML
    private void cancelEdit() {
        resetForm();
        pesanLabel.setText("Edit dibatalkan.");
    }

    private void resetForm() {
        editingId = 0;
        namaField.clear();
        hargaField.clear();
        fotoBox.setValue("roti.png");
        saveButton.setText("Simpan Produk");
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
    }

    /** Menambahkan produk baru atau menyimpan perubahan produk yang diedit. */
    @FXML
    private void saveProduct() {
        final Produk produk;
        try {
            String nama = namaField.getText();
            String teksHarga = hargaField.getText();
            if (teksHarga == null || teksHarga.isBlank()) {
                pesanLabel.setText("Harga produk wajib diisi.");
                return;
            }
            long harga = Long.parseLong(teksHarga.trim());
            String foto = fotoBox.getValue();
            produk = new Produk(editingId, nama, harga, foto);
        } catch (NumberFormatException e) {
            pesanLabel.setText("Harga harus angka bulat, contoh 12000.");
            return;
        } catch (IllegalArgumentException e) {
            pesanLabel.setText(e.getMessage());
            return;
        }

        try {
            if (editingId == 0) {
                repository.add(produk);
                pesanLabel.setText("Produk berhasil disimpan.");
            } else {
                repository.update(produk);
                pesanLabel.setText("Produk berhasil diperbarui.");
            }
            resetForm();
            showData();
        } catch (SQLException e) {
            pesanLabel.setText("Gagal menyimpan data. Lihat Output.");
            e.printStackTrace();
        }
    }
}
