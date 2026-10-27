# LED Editor — 8×8×8 RGB LED 光立方動畫編輯器

以 **JavaFX 3D** 製作的桌面工具，用來編輯 8×8×8 RGB LED 光立方的動畫序列，並匯出成 Arduino 可以直接 `#include` 的 `.h` 標頭檔。

這是配合大學課程製作的輔助工具。實際讀取資料並控制 LED 輸出的是另一支 Arduino 程式，不在這個 repo 裡。

```
LED Editor（本專案） ──匯出 .h──▶ Arduino 程式 ──▶ 8×8×8 LED 光立方
```

## 功能

### 3D 編輯

- 以 512 顆 3D 方塊呈現整個光立方，點擊任一顆 LED 即可設定顏色
- 右鍵拖曳旋轉視角、滾輪縮放，點右上角座標軸可重設視角
- 檢視模式：
  - **XY / XZ / YZ**：一次只顯示一層，可逐層切換，方便編輯內部的 LED
  - **All**：顯示整顆立方體，隱藏未點亮的 LED
- 可開關外框，只畫出面向鏡頭的三個面，方便辨識邊界
- 點擊 LED 時，標題列會顯示座標和對應到實體燈條上的序號，方便對照硬體除錯

### 顏色

- 以 **色相（Hue）+ 彩度（Chroma）** 兩條滑桿選色，亮度固定為最亮，可直接切換是否點亮

### 動畫序列

- 每一步（Step）是一個完整的 8×8×8 畫面，各自設定停留時間（0.1 ～ 10 秒，間隔 0.1 秒）
- 可新增、複製、刪除、重新命名、上下調整順序
- 播放 / 暫停 / 停止預覽，播放中的步驟會顯示進度條

### 補間動畫（Extended steps）

自動在相鄰兩步之間產生每 0.1 秒一格的中間畫面，讓動畫從「切換」變成「流動」。最後一步會補間回第一步，可以無縫循環。演算法在 [`StepInterpolator.java`](src/customObject/StepInterpolator.java)：

1. **配對**：把前一步每顆亮著的 LED 配對到下一步最近的亮點；下一步沒被配到的亮點，再反向配對到前一步最近的亮點，確保兩邊的亮點都有軌跡
2. **移動與漸變**：每組配對形成一條軌跡，位置線性移動；色相沿色環的最短方向旋轉，彩度線性變化
3. **分配到格點**：軌跡的位置大多落在格點之間，用三線性插值（trilinear）把顏色按權重分給周圍 8 個格點
4. **混色**：同一格收到多條軌跡時，色相以向量方式做加權平均（避免 350° 和 10° 平均成 180°），權重太低的格子視為不亮

### 匯入 / 匯出

- **Export**：輸出 Arduino 用的 `.h` 檔
- **Import**：讀回 `.h` 檔繼續編輯
- **Import and concatenate**：把另一個檔案的步驟接在目前序列後面，方便合併多段動畫
- 匯出前會檢查步驟數上限（1500 步），避免超出 Arduino 的記憶體

## 匯出格式

```c
float PROGMEM delay_T[]={1.0,0.5,...};
uint32_t PROGMEM LED[][512]={
{0,16711680,0,...},
{...}
};
```

- `delay_T[i]`：第 i 步的停留秒數
- `LED[i]`：第 i 步的 512 顆 LED，每顆是 `0xRRGGBB` 的 24-bit 色碼，`0` 代表不亮
- 資料用 `PROGMEM` 存放在 Flash，省下 Arduino 有限的 SRAM
- LED 排列順序與實體燈條的走線一致：先依 X 分層，每層從最上排往下排，相鄰兩排方向相反（蛇形走線），Arduino 端可以照順序直接輸出，不用再轉換座標

## 技術

- Java 22 以上（使用 unnamed variable `_` 語法）
- JavaFX 24：3D 場景（`SubScene`、`PerspectiveCamera`、`Box`、`PhongMaterial`）、FXML、Timeline 動畫
- 匯入時用正規表示式解析 `.h` 檔的陣列內容

## 執行方式

需要 **JDK 22 以上**，以及另外下載的 [JavaFX SDK](https://gluonhq.com/products/javafx/)（24 以上）。

```bash
javac -encoding UTF-8 --module-path <JavaFX SDK 路徑>/lib --add-modules javafx.controls,javafx.fxml -d bin $(find src -name "*.java")
cp src/form/*.fxml bin/form/
java --module-path <JavaFX SDK 路徑>/lib --add-modules javafx.controls,javafx.fxml -cp bin form.MainApplication
```

也可以用 Eclipse 匯入專案（File → Import → Existing Projects into Workspace）。`.classpath` 裡的 JavaFX 路徑是我電腦上的位置，要改成自己的 JavaFX SDK 路徑。

## 專案結構

```
src/
├── customObject/
│   ├── Constants.java          # 常數、色相 / 彩度計算
│   ├── Step.java               # 單一步驟（8×8×8 顏色 + 停留時間）
│   └── StepInterpolator.java   # 補間動畫演算法
└── form/
    ├── MainApplication.java    # 程式進入點
    ├── MainController.java     # 主畫面：3D 場景、步驟管理、匯入匯出、播放
    ├── MainView.fxml
    ├── StepPaneController.java # 步驟清單中的單一項目
    ├── StepPaneView.fxml
    ├── ColorPickerController.java # 色相 / 彩度選色器
    └── ColorPickerView.fxml
```
