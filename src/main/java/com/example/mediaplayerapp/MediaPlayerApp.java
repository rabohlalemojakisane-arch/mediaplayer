package com.example.mediaplayerapp;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MediaPlayerApp extends Application {

    private Stage primaryStage;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ImageView albumArtView;
    private Label nowPlayingLabel;
    private Label timeLabel;
    private Label volumeLabel;
    private Button playPauseBtn;
    private Button muteBtn;

    private ListView<String> playlistView;
    private ObservableList<String> playlistItems = FXCollections.observableArrayList();
    private List<File> playlistFiles = new ArrayList<>();
    private int currentIndex = -1;

    private Slider volumeSlider;
    private Slider seekSlider;
    private boolean muted = false;
    private double previousVolume = 0.5;
    private boolean userSeeking = false;

    private boolean shuffleMode = false;
    private int repeatMode = 0;
    private boolean isFullscreen = false;
    private HBox equalizerBars;
    private Timeline eqAnimation;
    private Label toast;
    private ToggleButton shuffleBtn;
    private Button repeatBtn;
    private ComboBox<String> speedBox;
    private FadeTransition toastFade;
    private final Random random = new Random();

    private static final String BTN_RED =
            "-fx-background-color: #e94560;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10px 20px;" +
                    "-fx-background-radius: 20px;" +
                    "-fx-cursor: hand;";

    private static final String BTN_RED_HOVER =
            "-fx-background-color: #ff6b81;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10px 20px;" +
                    "-fx-background-radius: 20px;" +
                    "-fx-cursor: hand;";

    private static final String TOGGLE_OFF =
            "-fx-background-color: #4a4a6a;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10px 16px;" +
                    "-fx-background-radius: 20px;" +
                    "-fx-cursor: hand;";

    private static final String TOGGLE_ON =
            "-fx-background-color: #27ae60;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10px 16px;" +
                    "-fx-background-radius: 20px;" +
                    "-fx-cursor: hand;";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #12121c;");

        // ---------- Header ----------
        nowPlayingLabel = new Label("🎵 No media loaded");
        nowPlayingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        nowPlayingLabel.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(nowPlayingLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setStyle("-fx-background-color: #1e1e2f;");

        // ---------- Display Area ----------
        albumArtView = new ImageView();
        albumArtView.setPreserveRatio(true);
        albumArtView.setFitWidth(500);
        albumArtView.setFitHeight(420);
        albumArtView.setVisible(false);

        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        mediaView.setFitWidth(860);

        // Equalizer bars (overlay)
        equalizerBars = new HBox(4);
        equalizerBars.setAlignment(Pos.BOTTOM_CENTER);
        equalizerBars.setPadding(new Insets(0, 0, 20, 0));
        equalizerBars.setVisible(false);
        equalizerBars.setMouseTransparent(true);
        for (int i = 0; i < 16; i++) {
            Rectangle bar = new Rectangle(6, 10, Color.web("#e94560"));
            bar.setArcWidth(4);
            bar.setArcHeight(4);
            equalizerBars.getChildren().add(bar);
        }

        eqAnimation = new Timeline(new KeyFrame(Duration.millis(110), e -> {
            for (Node n : equalizerBars.getChildren()) {
                ((Rectangle) n).setHeight(5 + random.nextDouble() * 45);
            }
        }));
        eqAnimation.setCycleCount(Timeline.INDEFINITE);

        // Toast notification
        toast = new Label();
        toast.setStyle(
                "-fx-background-color: rgba(233,69,96,0.95);" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10px 22px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;"
        );
        toast.setVisible(false);
        toast.setMouseTransparent(true);

        StackPane videoPane = new StackPane(albumArtView, mediaView, equalizerBars, toast);
        videoPane.setStyle("-fx-background-color: black;");
        videoPane.setPrefHeight(420);
        videoPane.setMinHeight(300);
        StackPane.setAlignment(equalizerBars, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        StackPane.setMargin(toast, new Insets(15, 0, 0, 0));

        // ---------- Seek Slider ----------
        seekSlider = new Slider(0, 100, 0);
        seekSlider.setPrefWidth(860);
        seekSlider.setStyle("-fx-control-inner-background: #2a2a3e;");
        seekSlider.setOnMousePressed(e -> userSeeking = true);
        seekSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && !total.isUnknown()) {
                    mediaPlayer.seek(total.multiply(seekSlider.getValue() / 100.0));
                }
            }
            userSeeking = false;
        });

        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: #b0b0c0; -fx-font-size: 13px;");

        HBox seekBox = new HBox(10, seekSlider, timeLabel);
        seekBox.setAlignment(Pos.CENTER);
        seekBox.setPadding(new Insets(8, 20, 8, 20));
        seekBox.setStyle("-fx-background-color: #1e1e2f;");
        HBox.setHgrow(seekSlider, Priority.ALWAYS);

        VBox videoBox = new VBox(videoPane, seekBox);

        // ---------- Playlist ----------
        Label playlistTitle = new Label("🎵 PLAYLIST");
        playlistTitle.setStyle("-fx-text-fill: gold; -fx-font-size: 15px; -fx-font-weight: bold;");

        playlistView = new ListView<>(playlistItems);
        playlistView.setPrefWidth(260);
        playlistView.setStyle(
                "-fx-background-color: #1e1e2f;" +
                        "-fx-control-inner-background: #1e1e2f;" +
                        "-fx-text-fill: white;"
        );

        // Custom cell with context menu + double-click
        playlistView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: white; -fx-padding: 6px;");
                        if (getIndex() == currentIndex) {
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-padding: 6px;");
                        }
                    }
                }
            };

            cell.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !cell.isEmpty()) {
                    int idx = cell.getIndex();
                    if (idx >= 0 && idx < playlistFiles.size()) {
                        currentIndex = idx;
                        loadMedia(playlistFiles.get(idx), true);
                    }
                }
            });

            ContextMenu menu = new ContextMenu();
            MenuItem playItem = new MenuItem("▶ Play");
            playItem.setOnAction(e -> {
                int idx = cell.getIndex();
                if (idx >= 0 && idx < playlistFiles.size()) {
                    currentIndex = idx;
                    loadMedia(playlistFiles.get(idx), true);
                }
            });
            MenuItem removeItem = new MenuItem("➖ Remove");
            removeItem.setOnAction(e -> {
                int idx = cell.getIndex();
                if (idx >= 0) {
                    playlistView.getSelectionModel().select(idx);
                    removeSelected();
                }
            });
            menu.getItems().addAll(playItem, removeItem);
            cell.setContextMenu(menu);

            cell.setOnMouseEntered(e -> {
                if (!cell.isEmpty()) cell.setStyle(
                        "-fx-text-fill: white; -fx-padding: 6px; -fx-background-color: #2a2a3e;");
            });
            cell.setOnMouseExited(e -> {
                if (!cell.isEmpty()) cell.setStyle(
                        "-fx-text-fill: white; -fx-padding: 6px;");
            });

            return cell;
        });

        Button addBtn = createSmallButton("➕ Add");
        Button removeBtn = createSmallButton("➖ Remove");
        Button clearBtn = createSmallButton("🗑 Clear");
        addBtn.setOnAction(e -> addFiles());
        removeBtn.setOnAction(e -> removeSelected());
        clearBtn.setOnAction(e -> clearPlaylist());

        HBox playlistControls = new HBox(6, addBtn, removeBtn, clearBtn);
        playlistControls.setAlignment(Pos.CENTER);

        Label hintLabel = new Label("💡 Drag files onto window\nor double-click a track");
        hintLabel.setStyle("-fx-text-fill: #7a7a9a; -fx-font-size: 11px;");
        hintLabel.setAlignment(Pos.CENTER);
        hintLabel.setWrapText(true);

        VBox playlistBox = new VBox(10, playlistTitle, playlistView, playlistControls, hintLabel);
        playlistBox.setStyle("-fx-padding: 14px; -fx-background-color: #181826;");
        playlistBox.setPrefWidth(280);

        // ---------- Player Controls ----------
        Button prevBtn = createButton("⏮ Prev");
        playPauseBtn = createButton("▶ Play");
        Button stopBtn = createButton("⏹ Stop");
        Button nextBtn = createButton("⏭ Next");
        muteBtn = createButton("🔊");

        shuffleBtn = new ToggleButton("🔀");
        shuffleBtn.setStyle(TOGGLE_OFF);
        shuffleBtn.setOnAction(e -> {
            shuffleMode = shuffleBtn.isSelected();
            shuffleBtn.setStyle(shuffleMode ? TOGGLE_ON : TOGGLE_OFF);
            showToast(shuffleMode ? "🔀 Shuffle ON" : "🔀 Shuffle OFF");
        });

        repeatBtn = createButton("➡");
        repeatBtn.setOnAction(e -> {
            repeatMode = (repeatMode + 1) % 3;
            if (repeatMode == 0) {
                repeatBtn.setText("➡");
                showToast("🔁 Repeat OFF");
            } else if (repeatMode == 1) {
                repeatBtn.setText("🔁");
                showToast("🔁 Repeat ALL");
            } else {
                repeatBtn.setText("🔂");
                showToast("🔂 Repeat ONE");
            }
        });

        speedBox = new ComboBox<>();
        speedBox.getItems().addAll("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x");
        speedBox.setValue("1x");
        speedBox.setStyle("-fx-background-color: #4a4a6a; -fx-text-fill: white;");
        speedBox.setOnAction(e -> {
            if (mediaPlayer != null) {
                String v = speedBox.getValue().replace("x", "");
                mediaPlayer.setRate(Double.parseDouble(v));
            }
            showToast("⏩ Speed: " + speedBox.getValue());
        });

        Button fullscreenBtn = createButton("🖥");
        fullscreenBtn.setOnAction(e -> toggleFullscreen());

        prevBtn.setOnAction(e -> playPrevious());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        stopBtn.setOnAction(e -> stopMedia());
        nextBtn.setOnAction(e -> playNext());
        muteBtn.setOnAction(e -> toggleMute());

        volumeLabel = new Label("50%");
        volumeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-min-width: 40px;");

        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(110);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newVal.doubleValue());
            volumeLabel.setText((int) (newVal.doubleValue() * 100) + "%");
            muted = newVal.doubleValue() == 0;
        });

        HBox controls = new HBox(8,
                prevBtn, playPauseBtn, stopBtn, nextBtn,
                shuffleBtn, repeatBtn,
                speedBox, fullscreenBtn,
                muteBtn, volumeSlider, volumeLabel);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(14));
        controls.setStyle("-fx-background-color: #1e1e2f;");

        // ---------- Keyboard Hint Bar ----------
        Label hints = new Label(
                "⌨ Space: Play/Pause  |  S: Stop  |  N: Next  |  P: Previous  |  ↑/↓: Volume  |  M: Mute  |  F: Fullscreen"
        );
        hints.setStyle("-fx-text-fill: #7a7a9a; -fx-font-size: 11px;");

        HBox hintBar = new HBox(hints);
        hintBar.setAlignment(Pos.CENTER);
        hintBar.setPadding(new Insets(6));
        hintBar.setStyle("-fx-background-color: #0f0f18;");

        // ---------- Layout ----------
        HBox centerBox = new HBox(videoBox, playlistBox);
        HBox.setHgrow(videoBox, Priority.ALWAYS);

        VBox bottomBox = new VBox(controls, hintBar);
        root.setTop(header);
        root.setCenter(centerBox);
        root.setBottom(bottomBox);

        // ---------- Drag & Drop ----------
        root.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                root.setStyle("-fx-background-color: #1a1a35; -fx-border-color: #e94560; -fx-border-width: 3px;");
            }
            e.consume();
        });

        root.setOnDragExited(e -> root.setStyle("-fx-background-color: #12121c;"));

        root.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File f : db.getFiles()) {
                    if (isMediaFile(f)) {
                        playlistFiles.add(f);
                        playlistItems.add(f.getName());
                        success = true;
                    }
                }
                if (currentIndex == -1 && !playlistFiles.isEmpty()) {
                    currentIndex = 0;
                    playlistView.getSelectionModel().select(0);
                    loadMedia(playlistFiles.get(0), true);
                }
                if (success) showToast("✅ Added " + db.getFiles().size() + " file(s)");
            }
            e.setDropCompleted(success);
            root.setStyle("-fx-background-color: #12121c;");
            e.consume();
        });

        // ---------- Scene ----------
        Scene scene = new Scene(root, 1150, 680);
        primaryStage.setTitle("🎵 Media Player");
        primaryStage.setScene(scene);

        scene.setOnKeyPressed(e -> handleKeyPress(e.getCode()));

        primaryStage.show();
    }

    private boolean isMediaFile(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".mp3") || n.endsWith(".mp4") || n.endsWith(".m4a")
                || n.endsWith(".wav") || n.endsWith(".avi") || n.endsWith(".aac");
    }

    private void handleKeyPress(KeyCode code) {
        switch (code) {
            case SPACE: togglePlayPause(); break;
            case S: stopMedia(); break;
            case N: playNext(); break;
            case P: playPrevious(); break;
            case UP: increaseVolume(); break;
            case DOWN: decreaseVolume(); break;
            case M: toggleMute(); break;
            case F: toggleFullscreen(); break;
        }
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        primaryStage.setFullScreen(isFullscreen);
        if (isFullscreen) showToast("Press ESC to exit fullscreen");
    }

    private void showToast(String message) {
        toast.setText(message);
        toast.setVisible(true);
        toast.setOpacity(1.0);
        if (toastFade != null) toastFade.stop();
        toastFade = new FadeTransition(Duration.millis(500), toast);
        toastFade.setDelay(Duration.millis(1600));
        toastFade.setFromValue(1.0);
        toastFade.setToValue(0.0);
        toastFade.setOnFinished(e -> toast.setVisible(false));
        toastFade.play();
    }

    private void addFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add Media Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files", "*.mp4", "*.mp3", "*.m4a", "*.wav", "*.avi"),
                new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi"),
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.m4a", "*.wav"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);
        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                playlistFiles.add(file);
                playlistItems.add(file.getName());
            }
            showToast("✅ Added " + files.size() + " file(s)");
            if (currentIndex == -1 && !playlistFiles.isEmpty()) {
                currentIndex = 0;
                playlistView.getSelectionModel().select(0);
                loadMedia(playlistFiles.get(0), true);
            }
        }
    }

    private void removeSelected() {
        int idx = playlistView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        playlistItems.remove(idx);
        playlistFiles.remove(idx);
        if (idx == currentIndex) {
            stopMedia();
            if (mediaPlayer != null) { mediaPlayer.dispose(); mediaPlayer = null; }
            mediaView.setMediaPlayer(null);
            albumArtView.setVisible(false);
            albumArtView.setImage(null);
            nowPlayingLabel.setText("🎵 No media loaded");
            currentIndex = -1;
        } else if (idx < currentIndex) currentIndex--;
        showToast("➖ Removed");
    }

    private void clearPlaylist() {
        stopMedia();
        if (mediaPlayer != null) { mediaPlayer.dispose(); mediaPlayer = null; }
        mediaView.setMediaPlayer(null);
        albumArtView.setVisible(false);
        albumArtView.setImage(null);
        playlistItems.clear();
        playlistFiles.clear();
        currentIndex = -1;
        nowPlayingLabel.setText("🎵 No media loaded");
        timeLabel.setText("00:00 / 00:00");
        seekSlider.setValue(0);
        showToast("🗑 Playlist cleared");
    }

    private void loadMedia(File file, boolean autoPlay) {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        eqAnimation.stop();
        equalizerBars.setVisible(false);

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(volumeSlider.getValue());
            String v = speedBox.getValue().replace("x", "");
            mediaPlayer.setRate(Double.parseDouble(v));

            boolean isAudio = file.getName().toLowerCase().matches(".*\\.(mp3|m4a|wav|aac)$");

            if (isAudio) {
                Image art = extractAlbumArt(file);
                if (art == null) art = getCompanionImage(file);
                if (art == null) art = createMusicPlaceholder();
                albumArtView.setImage(art);
                albumArtView.setVisible(true);
                mediaView.setVisible(false);

                FadeTransition ft = new FadeTransition(Duration.millis(400), albumArtView);
                ft.setFromValue(0.3);
                ft.setToValue(1.0);
                ft.play();
            } else {
                albumArtView.setVisible(false);
                albumArtView.setImage(null);
                mediaView.setVisible(true);
            }

            mediaPlayer.setOnReady(() -> {
                nowPlayingLabel.setText("🎵 Now Playing: " + file.getName());
                updateTimeLabel();
                if (autoPlay) {
                    mediaPlayer.play();
                    playPauseBtn.setText("⏸ Pause");
                }
            });

            mediaPlayer.statusProperty().addListener((obs, oldS, newS) -> {
                if (newS == MediaPlayer.Status.PLAYING) {
                    eqAnimation.play();
                    equalizerBars.setVisible(isAudio);
                } else {
                    eqAnimation.pause();
                    for (Node n : equalizerBars.getChildren()) ((Rectangle) n).setHeight(10);
                    if (newS == MediaPlayer.Status.STOPPED) equalizerBars.setVisible(false);
                }
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                if (!userSeeking) { updateSeekSlider(newVal); updateTimeLabel(); }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                if (repeatMode == 2) {
                    mediaPlayer.seek(Duration.ZERO);
                    mediaPlayer.play();
                } else if (repeatMode == 0 && currentIndex == playlistFiles.size() - 1 && !shuffleMode) {
                    stopMedia();
                    showToast("🎵 Playlist finished");
                } else {
                    playNext();
                }
            });

            mediaPlayer.setOnError(() ->
                    nowPlayingLabel.setText("⚠ Error: " + mediaPlayer.getError().getMessage())
            );

            showToast("🎵 " + file.getName());

        } catch (Exception ex) {
            nowPlayingLabel.setText("⚠ Failed to load: " + ex.getMessage());
        }
    }

    private Image extractAlbumArt(File file) {
        try {
            org.jaudiotagger.audio.AudioFile audioFile =
                    org.jaudiotagger.audio.AudioFileIO.read(file);
            org.jaudiotagger.tag.Tag tag = audioFile.getTag();
            if (tag == null) return null;
            List<org.jaudiotagger.tag.images.Artwork> artworks = tag.getArtworkList();
            if (artworks == null || artworks.isEmpty()) return null;
            byte[] data = artworks.get(0).getBinaryData();
            if (data == null || data.length == 0) return null;
            return new Image(new ByteArrayInputStream(data));
        } catch (Exception e) { return null; }
    }

    private Image getCompanionImage(File mediaFile) {
        String base = mediaFile.getName().replaceAll("\\.[^.]+$", "");
        File dir = mediaFile.getParentFile();
        if (dir == null) return null;
        for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
            File img = new File(dir, base + ext);
            if (img.exists()) {
                try { return new Image(img.toURI().toString()); }
                catch (Exception e) { return null; }
            }
        }
        return null;
    }

    private Image createMusicPlaceholder() {
        Canvas canvas = new Canvas(500, 500);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        LinearGradient grad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1e1e2f")),
                new Stop(1, Color.web("#533483")));
        gc.setFill(grad);
        gc.fillRect(0, 0, 500, 500);
        gc.setFill(Color.web("#e94560"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 180));
        gc.fillText("🎵", 160, 320);
        gc.setFill(Color.web("#b0b0c0"));
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 22));
        gc.fillText("Now Playing", 170, 400);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage img = canvas.snapshot(params, null);
        return img;
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playPauseBtn.setText("▶ Play");
        } else {
            mediaPlayer.play();
            playPauseBtn.setText("⏸ Pause");
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        mediaPlayer.stop();
        mediaPlayer.seek(Duration.ZERO);
        playPauseBtn.setText("▶ Play");
        eqAnimation.pause();
        equalizerBars.setVisible(false);
    }

    private void playNext() {
        if (playlistFiles.isEmpty()) return;
        if (shuffleMode) {
            int idx = random.nextInt(playlistFiles.size());
            if (playlistFiles.size() > 1) {
                while (idx == currentIndex) idx = random.nextInt(playlistFiles.size());
            }
            currentIndex = idx;
        } else {
            currentIndex = (currentIndex + 1) % playlistFiles.size();
        }
        playlistView.getSelectionModel().select(currentIndex);
        playlistView.refresh();
        loadMedia(playlistFiles.get(currentIndex), true);
    }

    private void playPrevious() {
        if (playlistFiles.isEmpty()) return;
        if (shuffleMode) {
            int idx = random.nextInt(playlistFiles.size());
            if (playlistFiles.size() > 1) {
                while (idx == currentIndex) idx = random.nextInt(playlistFiles.size());
            }
            currentIndex = idx;
        } else {
            currentIndex = (currentIndex - 1 + playlistFiles.size()) % playlistFiles.size();
        }
        playlistView.getSelectionModel().select(currentIndex);
        playlistView.refresh();
        loadMedia(playlistFiles.get(currentIndex), true);
    }

    private void increaseVolume() {
        double v = Math.min(1.0, volumeSlider.getValue() + 0.05);
        volumeSlider.setValue(v);
        if (mediaPlayer != null) mediaPlayer.setVolume(v);
        muted = false;
    }

    private void decreaseVolume() {
        double v = Math.max(0.0, volumeSlider.getValue() - 0.05);
        volumeSlider.setValue(v);
        if (mediaPlayer != null) mediaPlayer.setVolume(v);
        muted = (v == 0);
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        if (!muted) {
            previousVolume = volumeSlider.getValue();
            volumeSlider.setValue(0);
            mediaPlayer.setVolume(0);
            muted = true;
            muteBtn.setText("🔇");
            showToast("🔇 Muted");
        } else {
            volumeSlider.setValue(previousVolume);
            mediaPlayer.setVolume(previousVolume);
            muted = false;
            muteBtn.setText("🔊");
            showToast("🔊 Unmuted");
        }
    }

    private void updateSeekSlider(Duration current) {
        Duration total = mediaPlayer.getTotalDuration();
        if (total != null && !total.isUnknown() && total.toMillis() > 0) {
            seekSlider.setValue((current.toMillis() / total.toMillis()) * 100);
        }
    }

    private void updateTimeLabel() {
        if (mediaPlayer == null) return;
        Duration current = mediaPlayer.getCurrentTime();
        Duration total = mediaPlayer.getTotalDuration();
        timeLabel.setText(format(current) + " / " + format(total));
    }

    private String format(Duration d) {
        if (d == null || d.isUnknown()) return "00:00";
        int m = (int) d.toMinutes();
        int s = (int) (d.toSeconds() % 60);
        return String.format("%02d:%02d", m, s);
    }

    private Button createButton(String text) {
        Button b = new Button(text);
        b.setStyle(BTN_RED);
        b.setOnMouseEntered(e -> b.setStyle(BTN_RED_HOVER));
        b.setOnMouseExited(e -> b.setStyle(BTN_RED));
        return b;
    }

    private Button createSmallButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: #4a4a6a;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6px 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-cursor: hand;");
        return b;
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}