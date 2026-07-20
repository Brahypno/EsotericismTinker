package org.brahypno.esotericismtinker.transcendence.table.client;

final class TranscendenceLeftStationLayout {
    static final int WIDTH = 126;
    static final int HEIGHT = 126;
    static final int ROW_HEIGHT = 13;
    static final int ROOT_BUTTON_HEIGHT = 14;
    static final int ROOT_BUTTON_GAP = 2;

    private static final int OUTER_PADDING = 7;
    private static final int TITLE_TOP = 7;
    private static final int TITLE_HEIGHT = 12;
    private static final int META_TOP = 23;
    private static final int META_HEIGHT = 12;
    private static final int RECEPTION_TOP = 42;
    private static final int BOTTOM_PADDING = 7;

    private Region panel = Region.EMPTY;
    private Region title = Region.EMPTY;
    private Region back = Region.EMPTY;
    private Region substrate = Region.EMPTY;
    private Region content = Region.EMPTY;
    private Region scrollbar = Region.EMPTY;

    void update(int left, int top, int width, int height) {
        panel = new Region(left, top, width, height);
        title = new Region(left + 20, top + TITLE_TOP, width - 40, TITLE_HEIGHT);
        back = new Region(left + OUTER_PADDING, top + META_TOP, 14, META_HEIGHT);
        substrate = new Region(
                back.right() + 4,
                top + META_TOP + 1,
                Math.max(0, left + width - OUTER_PADDING - (back.right() + 4)),
                META_HEIGHT
        );
        content = new Region(
                left + OUTER_PADDING,
                top + RECEPTION_TOP,
                width - OUTER_PADDING * 2,
                Math.max(0, height - RECEPTION_TOP - BOTTOM_PADDING)
        );
        scrollbar = new Region(left + width - 4, content.y(), 2, content.height());
    }

    Region panel() { return panel; }
    Region title() { return title; }
    Region backButton() { return back; }
    Region substrate() { return substrate; }
    Region content() { return content; }
    Region scrollbar() { return scrollbar; }

    Region rootEntry(int index) {
        int count = 4;
        int totalHeight = count * ROOT_BUTTON_HEIGHT + (count - 1) * ROOT_BUTTON_GAP;
        int firstY = content.y() + Math.max(0, content.height() - totalHeight);
        return new Region(
                content.x() + 1,
                firstY + index * (ROOT_BUTTON_HEIGHT + ROOT_BUTTON_GAP),
                Math.max(0, content.width() - 2),
                ROOT_BUTTON_HEIGHT
        );
    }

    int visibleReceptionRows() {
        return Math.max(1, content.height() / ROW_HEIGHT);
    }

    ReceptionRow receptionRow(int visibleIndex) {
        int rowTop = content.y() + visibleIndex * ROW_HEIGHT;
        int buttonWidth = 18;
        int buttonGap = 2;
        int plusX = content.right() - buttonWidth;
        int minusX = plusX - buttonGap - buttonWidth;
        int labelRight = minusX - 4;

        return new ReceptionRow(
                new Region(content.x(), rowTop, content.width(), 12),
                new Region(content.x() + 1, rowTop, Math.max(0, labelRight - content.x() - 1), 12),
                new Region(minusX, rowTop, buttonWidth, 12),
                new Region(plusX, rowTop, buttonWidth, 12)
        );
    }

    boolean contains(double mouseX, double mouseY) {
        return panel.contains(mouseX, mouseY);
    }

    record ReceptionRow(Region row, Region label, Region minusButton, Region plusButton) {}

    record Region(int x, int y, int width, int height) {
        static final Region EMPTY = new Region(0, 0, 0, 0);
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
