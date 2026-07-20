package org.brahypno.esotericismtinker.transcendence.table.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.network.EsotericismTinkerNetwork;
import org.brahypno.esotericismtinker.network.TranscendenceInvestiturePacket;
import org.brahypno.esotericismtinker.network.TranscendenceReceptionPacket;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonAllocationLogic;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonData;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonDatabase;
import org.brahypno.esotericismtinker.transcendence.intrinsic.NoumenonInvestitureLogic;
import org.brahypno.esotericismtinker.transcendence.table.block.TranscendenceAnvilBlockEntity;
import org.brahypno.esotericismtinker.transcendence.table.menu.TranscendenceAnvilMenu;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.layout.LayoutIcon;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tables.client.inventory.ToolTableScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TranscendenceAnvilScreen
        extends ToolTableScreen<TranscendenceAnvilBlockEntity, TranscendenceAnvilMenu> {
    private static final Component IDLE_TITLE =
            Component.translatable("gui.esotericism_tinker.transcendence_anvil.idle.title");
    private static final Component IDLE_DESCRIPTION =
            Component.translatable("gui.esotericism_tinker.transcendence_anvil.idle.description");

    private static final Component ASCII_HAMMER = Component.literal("\n\n")
                                                           .append("             .\n")
                                                           .append(" /( _________\n")
                                                           .append(" | >:=========`\n")
                                                           .append(" )( \n")
                                                           .append(" \"\"")
                                                           .withStyle(ChatFormatting.DARK_GRAY);

    private static final Component ASCII_LOCK = Component.literal("\n\n")
                                                         .append("     .----.\n")
                                                         .append("    /      \\\n")
                                                         .append("    |      |\n")
                                                         .append(" .----------.\n")
                                                         .append(" |    ()    |\n")
                                                         .append(" |    /\\    |\n")
                                                         .append(" '----------'")
                                                         .withStyle(ChatFormatting.DARK_GRAY);

    private static final ResourceLocation TINKER_TEXTURE =
            TConstruct.getResource("textures/gui/tinker.png");

    private static final ElementScreen SLOT_BACKGROUND =
            new ElementScreen(TINKER_TEXTURE, 176, 0, 18, 18, 256, 256);
    private static final ElementScreen SLOT_BORDER =
            new ElementScreen(TINKER_TEXTURE, 194, 0, 18, 18, 256, 256);
    private static final ElementScreen ACTIVE_TEXT_FIELD =
            new ElementScreen(TINKER_TEXTURE, 0, 232, 90, 12, 256, 256);
    private static final ElementScreen ITEM_COVER =
            ACTIVE_TEXT_FIELD.move(176, 18, 70, 64);
    private static final ElementScreen PANEL_SPACE_LEFT =
            ACTIVE_TEXT_FIELD.move(0, 196, 5, 4).shift(36, 0);
    private static final ElementScreen PANEL_SPACE_RIGHT =
            ACTIVE_TEXT_FIELD.move(9, 196, 9, 4).shift(36, 0);
    private static final ElementScreen LEFT_BEAM =
            ACTIVE_TEXT_FIELD.move(0, 202, 2, 7).shift(0, 7);
    private static final ElementScreen RIGHT_BEAM =
            ACTIVE_TEXT_FIELD.move(131, 202, 2, 7).shift(0, 7);
    private static final ScalableElementScreen CENTER_BEAM =
            new ScalableElementScreen(TINKER_TEXTURE, 2, 209, 129, 7, 256, 256);

    private static final int INFO_PANEL_HEIGHT = 83;

    private final TranscendenceLeftInfoPanel leftInfo;
    private final Inventory playerInventory;
    private final StationSlotLayout stationLayout;
    private final TranscendenceLeftStationLayout leftLayout = new TranscendenceLeftStationLayout();
    private final List<AbstractWidget> leftWidgets = new ArrayList<>();
    private final List<ReceptionButtons> receptionButtons = new ArrayList<>();
    private final List<SlotType> receptionTypes = new ArrayList<>();
    private final List<ModifierEntry> investitureTraits = new ArrayList<>();

    private TranscendenceLeftPage leftPage = TranscendenceLeftPage.ROOT;
    private int receptionScroll;
    private int investitureScroll;
    private ItemStack lastInvestitureSource = ItemStack.EMPTY;
    private ResourceLocation lastSelectedInvestiture;
    private boolean positiveAspectDisplayed;

    public TranscendenceAnvilScreen(
            TranscendenceAnvilMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        imageHeight = 184;
        playerInventory = inventory;
        stationLayout = menu.getStationLayout();

        tinkerInfo.metal();
        modifierInfo.metal();

        leftInfo = new TranscendenceLeftInfoPanel(this, menu, playerInventory, title);
        leftInfo.metal();
        leftInfo.setTextScale(7 / 9F);
        addModule(leftInfo);

        receptionTypes.addAll(
                SlotType.getAllSlotTypes()
                        .stream()
                        .filter(type -> NoumenonDatabase.getReceptionCost(type.getName()) > 0)
                        .sorted(Comparator.comparing(type -> type.getDisplayName().getString()))
                        .toList()
        );
    }

    @Override
    protected void init() {
        tinkerInfo.xOffset = 2;
        tinkerInfo.yOffset = CENTER_BEAM.h + PANEL_SPACE_LEFT.h;

        modifierInfo.xOffset = 2;
        modifierInfo.yOffset =
                tinkerInfo.yOffset + INFO_PANEL_HEIGHT + 4;

        /*
         * imageWidth 在一次初始化结束后可能已经包含左右 ModuleScreen，
         * 因此不能用它计算左侧面板偏移。
         *
         * realWidth 是 MultiModuleScreen 保存的中央主体 GUI 宽度。
         * 首次初始化时 realWidth 尚未设置，才回退到 imageWidth。
         */
        int parentWidth = realWidth > 0 ? realWidth : imageWidth;

        leftInfo.xOffset = -(parentWidth + TranscendenceLeftStationLayout.WIDTH + 2);
        leftInfo.yOffset = CENTER_BEAM.h + PANEL_SPACE_LEFT.h;

        super.init();

        setupArmorStandPreview(-55, 195, 35);

        var leftArea = leftInfo.getArea();
        leftLayout.update(
                leftArea.getX(),
                leftArea.getY(),
                leftArea.getWidth(),
                leftArea.getHeight()
        );

        clampReceptionScroll();

        positiveAspectDisplayed = hasPositiveAspect(getShownTool());

        rebuildLeftWidgets();
        updateDisplay();
    }

    private void clearLeftWidgets() {
        for (AbstractWidget widget : leftWidgets)
            removeWidget(widget);
        leftWidgets.clear();
        receptionButtons.clear();
    }

    private Button addLeftButton(
            Component label,
            int x,
            int y,
            int width,
            int height,
            Button.OnPress action
    ) {
        Button button = Button.builder(label, action)
                              .bounds(x, y, width, height)
                              .build();
        addRenderableWidget(button);
        leftWidgets.add(button);
        return button;
    }

    private void rebuildLeftWidgets() {
        clearLeftWidgets();

        LazyToolStack shown = getShownTool();
        boolean positive = hasPositiveAspect(shown);

        if (!positive){
            leftPage = TranscendenceLeftPage.ROOT;
            receptionScroll = 0;
            return;
        }

        if (leftPage == TranscendenceLeftPage.ROOT){
            Component[] labels = {
                    Component.translatable("gui.esotericism_tinker.transcendence_anvil.reception"),
                    Component.translatable("gui.esotericism_tinker.transcendence_anvil.sublimation"),
                    Component.translatable("gui.esotericism_tinker.transcendence_anvil.tuning"),
                    Component.translatable("gui.esotericism_tinker.transcendence_anvil.investiture")
            };
            TranscendenceLeftPage[] pages = {
                    TranscendenceLeftPage.RECEPTION,
                    TranscendenceLeftPage.SUBLIMATION,
                    TranscendenceLeftPage.TUNING,
                    TranscendenceLeftPage.INVESTITURE
            };

            for (int index = 0; index < labels.length; index++) {
                var bounds = leftLayout.rootEntry(index);
                TranscendenceLeftPage target = pages[index];
                addLeftButton(
                        labels[index],
                        bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                        button -> openLeftPage(target)
                );
            }
            return;
        }

        var backBounds = leftLayout.backButton();
        addLeftButton(
                Component.literal("<"),
                backBounds.x(), backBounds.y(), backBounds.width(), backBounds.height(),
                button -> openLeftPage(TranscendenceLeftPage.ROOT)
        );

        if (leftPage == TranscendenceLeftPage.RECEPTION){
            rebuildReceptionWidgets();
        }else if (leftPage == TranscendenceLeftPage.INVESTITURE){
            rebuildInvestitureWidgets();
        }
    }

    private void openLeftPage(TranscendenceLeftPage page) {
        leftPage = page;
        if (page != TranscendenceLeftPage.RECEPTION){
            receptionScroll = 0;
        }
        if (page != TranscendenceLeftPage.INVESTITURE){
            investitureScroll = 0;
        }
        rebuildLeftWidgets();
        refreshLeftPanel();
    }

    private void rebuildReceptionWidgets() {
        clampReceptionScroll();
        int visibleRows = leftLayout.visibleReceptionRows();
        int end = Math.min(receptionTypes.size(), receptionScroll + visibleRows);

        for (int index = receptionScroll; index < end; index++) {
            SlotType type = receptionTypes.get(index);
            int visibleIndex = index - receptionScroll;
            var row = leftLayout.receptionRow(visibleIndex);

            Button minus = addLeftButton(
                    Component.literal("-"),
                    row.minusButton().x(), row.minusButton().y(),
                    row.minusButton().width(), row.minusButton().height(),
                    button -> changeReception(type, -1)
            );
            Button plus = addLeftButton(
                    Component.literal("+"),
                    row.plusButton().x(), row.plusButton().y(),
                    row.plusButton().width(), row.plusButton().height(),
                    button -> changeReception(type, 1)
            );

            String slotType = type.getName();
            minus.active = menu.getPendingReception(slotType)
                           > menu.getInitialReception(slotType)
                           && validateReceptionChange(slotType, -1) == null;
            plus.active = validateReceptionChange(slotType, 1) == null;
            receptionButtons.add(new ReceptionButtons(type, minus, plus));
        }
    }

    private void refreshReceptionButtons() {
        for (ReceptionButtons buttons : receptionButtons) {
            String slotType = buttons.type().getName();
            buttons.minus().active = menu.getPendingReception(slotType)
                                     > menu.getInitialReception(slotType)
                                     && validateReceptionChange(slotType, -1) == null;
            buttons.plus().active = validateReceptionChange(slotType, 1) == null;
        }
    }

    private void clampReceptionScroll() {
        int maximum = Math.max(0, receptionTypes.size() - leftLayout.visibleReceptionRows());
        receptionScroll = Math.max(0, Math.min(receptionScroll, maximum));
    }

    private void changeReception(SlotType slotType, int delta) {
        if (validateReceptionChange(slotType.getName(), delta) != null){
            return;
        }
        EsotericismTinkerNetwork.CHANNEL.sendToServer(
                new TranscendenceReceptionPacket(
                        menu.containerId,
                        slotType.getName(),
                        delta
                )
        );
    }

    private Component validateReceptionChange(String slotType, int delta) {
        LazyToolStack input = menu.getInputTool();
        if (input == null || input.getStack().isEmpty()){
            return Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.reception.no_preview"
            );
        }

        int initial = menu.getInitialReception(slotType);
        int target = menu.getPendingReception(slotType) + delta;
        if (target < initial){
            return Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.reception.committed"
            );
        }

        ToolStack candidate = input.getTool().copy();
        return NoumenonAllocationLogic.validateAndApply(candidate, data -> {
            data.receptionSlots.clear();
            for (SlotType type : receptionTypes) {
                String name = type.getName();
                int value = name.equals(slotType) ? target : menu.getPendingReception(name);
                if (value > 0){
                    data.receptionSlots.put(name, value);
                }
            }
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (leftPage == TranscendenceLeftPage.RECEPTION
            && leftLayout.contains(mouseX, mouseY)
            && receptionTypes.size() > leftLayout.visibleReceptionRows()){
            int old = receptionScroll;
            receptionScroll -= (int) Math.signum(delta);
            clampReceptionScroll();
            if (old != receptionScroll){
                rebuildLeftWidgets();
                refreshLeftPanel();
            }
            return true;
        }
        if (leftPage == TranscendenceLeftPage.INVESTITURE
            && leftLayout.contains(mouseX, mouseY)
            && investitureTraits.size() > leftLayout.visibleReceptionRows()){
            int old = investitureScroll;
            investitureScroll -= (int) Math.signum(delta);
            clampInvestitureScroll();
            if (old != investitureScroll){
                rebuildLeftWidgets();
                refreshLeftPanel();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void updateDisplay() {
        TranscendenceAnvilBlockEntity anvil = menu.getTile();
        if (anvil == null){
            return;
        }

        LazyToolStack result = anvil.getResult();

        Component currentError = anvil.getCurrentError();
        if (currentError != null){
            error(currentError);
            refreshLeftPanel();
            return;
        }

        LazyToolStack shown =
                result == null ? anvil.getTool() : result;

        syncLeftWidgetState(shown);

        if (shown == null || shown.getStack().isEmpty()){
            updateArmorStandPreview(ItemStack.EMPTY);
            tinkerInfo.setCaption(IDLE_TITLE);
            tinkerInfo.setText(IDLE_DESCRIPTION);
            modifierInfo.setCaption(Component.empty());
            modifierInfo.setText(ASCII_HAMMER);
            refreshLeftPanel();
            return;
        }

        updateArmorStandPreview(shown.getStack());
        updateToolPanel(shown);
        updateModifierPanel(shown.getTool());
        refreshLeftPanel();
    }

    @Override
    public void error(@NotNull Component message) {
        this.tinkerInfo.setCaption(COMPONENT_ERROR);
        this.tinkerInfo.setText(message);
        this.modifierInfo.setCaption(Component.empty());
        this.modifierInfo.setText(Component.empty());
    }

    private void syncLeftWidgetState(LazyToolStack shown) {
        boolean positive = hasPositiveAspect(shown);
        if (positive == positiveAspectDisplayed){
            return;
        }
        positiveAspectDisplayed = positive;
        leftPage = TranscendenceLeftPage.ROOT;
        receptionScroll = 0;
        investitureScroll = 0;
        rebuildLeftWidgets();
    }

    private static boolean hasPositiveAspect(LazyToolStack shown) {
        return shown != null
               && !shown.getStack().isEmpty()
               && NoumenonData.read(shown.getTool()).level > 0;
    }

    private LazyToolStack getShownTool() {
        LazyToolStack shown = menu.getResultTool();
        return shown == null ? menu.getInputTool() : shown;
    }

    private void refreshLeftPanel() {
        LazyToolStack shown = getShownTool();

        if (shown == null || shown.getStack().isEmpty()){
            leftInfo.setCaption(
                    Component.translatable(
                            "gui.esotericism_tinker.transcendence_anvil.positive_aspect"
                    )
            );
            leftInfo.setText(ASCII_LOCK);
            return;
        }

        NoumenonData data = NoumenonData.read(shown.getTool());
        if (data.level <= 0){
            leftInfo.setCaption(
                    Component.translatable(
                            "gui.esotericism_tinker.transcendence_anvil.positive_aspect"
                    )
            );
            leftInfo.setText(ASCII_LOCK);
            return;
        }

        if (leftPage == TranscendenceLeftPage.ROOT){
            leftInfo.setCaption(
                    Component.translatable(
                            "gui.esotericism_tinker.transcendence_anvil.positive_aspect"
                    )
            );
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.level",
                    data.level
            ).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.substrate_points",
                    Component.translatable("noumenon.esotericism_tinker.substrate"),
                    data.remainingSubstratePoints(),
                    data.substratePoints
            ).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.substrate_points",
                    Component.translatable("noumenon.esotericism_tinker.elevation"),
                    data.remainingElevationPoints(),
                    data.elevationPoints
            ).withStyle(ChatFormatting.GOLD));
            lines.add(Component.empty());
            lines.add(Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.choose_category"
            ).withStyle(ChatFormatting.DARK_GRAY));
            leftInfo.setText(lines);
            return;
        }

        leftInfo.setCaption(pageTitle(leftPage));

        if (leftPage == TranscendenceLeftPage.RECEPTION){
            leftInfo.setText(Component.empty());
            return;
        }

        if (leftPage == TranscendenceLeftPage.INVESTITURE){
            LazyToolStack source = menu.getOffhandTool();
            if (source == null || source.getStack().isEmpty()){
                leftInfo.setText(Component.translatable(
                        "gui.esotericism_tinker.transcendence_anvil.investiture.offhand"
                ).withStyle(ChatFormatting.DARK_GRAY));
            }else if (investitureTraits.isEmpty()){
                leftInfo.setText(Component.translatable(
                        "gui.esotericism_tinker.transcendence_anvil.investiture.invalid"
                ).withStyle(ChatFormatting.DARK_GRAY));
            }else if (!canSelectInvestiture()){
                leftInfo.setText(Component.translatable(
                        "gui.esotericism_tinker.transcendence_anvil.investiture.locked"
                ).withStyle(ChatFormatting.DARK_GRAY));
            }else {
                leftInfo.setText(Component.empty());
            }
            return;
        }

        leftInfo.setText(Component.empty());
    }

    private static Component pageTitle(TranscendenceLeftPage page) {
        return switch (page) {
            case RECEPTION -> Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.reception"
            );
            case SUBLIMATION -> Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.sublimation"
            );
            case TUNING -> Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.tuning"
            );
            case INVESTITURE -> Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.investiture"
            );
            default -> Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.positive_aspect"
            );
        };
    }

    private static void renderIcon(
            GuiGraphics graphics,
            LayoutIcon icon,
            int x,
            int y
    ) {
        Pattern pattern = icon.getValue(Pattern.class);
        if (pattern != null){
            GuiUtil.renderPattern(graphics, pattern, x, y);
        }else {
            ItemStack stack = icon.getValue(ItemStack.class);
            if (stack != null){
                graphics.renderItem(stack, x, y);
            }
        }
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        drawBackground(graphics, TINKER_TEXTURE);

        final float scale = 3.7F;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(12.5F, 22.0F, 0.0F);
        pose.scale(scale, scale, 1.0F);
        renderIcon(
                graphics,
                stationLayout.getIcon(),
                (int) (cornerX / scale),
                (int) (cornerY / scale)
        );
        pose.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.82F);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        ITEM_COVER.draw(graphics, cornerX + 7, cornerY + 18);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.28F);
        for (int index = TranscendenceAnvilMenu.TOOL_SLOT;
             index < TranscendenceAnvilMenu.RESULT_SLOT;
             index++) {
            Slot slot = menu.getSlot(index);
            SLOT_BACKGROUND.draw(
                    graphics,
                    cornerX + slot.x - 1,
                    cornerY + slot.y - 1
            );
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int index = TranscendenceAnvilMenu.TOOL_SLOT;
             index < TranscendenceAnvilMenu.RESULT_SLOT;
             index++) {
            Slot slot = menu.getSlot(index);
            SLOT_BORDER.draw(
                    graphics,
                    cornerX + slot.x - 1,
                    cornerY + slot.y - 1
            );
        }

        var tinkerArea = tinkerInfo.getArea();
        int x = tinkerArea.getX() - LEFT_BEAM.w;
        int y = cornerY;
        LEFT_BEAM.draw(graphics, x, y);
        x += LEFT_BEAM.w;
        x += CENTER_BEAM.drawScaledX(graphics, x, y, tinkerArea.getWidth());
        RIGHT_BEAM.draw(graphics, x, y);

        PANEL_SPACE_LEFT.draw(
                graphics,
                tinkerArea.getX() + 5,
                tinkerArea.getY() - PANEL_SPACE_LEFT.h
        );
        PANEL_SPACE_RIGHT.draw(
                graphics,
                tinkerArea.getX()
                + tinkerArea.getWidth()
                - 5
                - PANEL_SPACE_RIGHT.w,
                tinkerArea.getY() - PANEL_SPACE_RIGHT.h
        );

        var modifierArea = modifierInfo.getArea();
        PANEL_SPACE_LEFT.draw(
                graphics,
                modifierArea.getX() + 5,
                modifierArea.getY() - PANEL_SPACE_LEFT.h
        );
        PANEL_SPACE_RIGHT.draw(
                graphics,
                modifierArea.getX()
                + modifierArea.getWidth()
                - 5
                - PANEL_SPACE_RIGHT.w,
                modifierArea.getY() - PANEL_SPACE_RIGHT.h
        );

        for (int index = TranscendenceAnvilMenu.TOOL_SLOT;
             index < TranscendenceAnvilMenu.RESULT_SLOT;
             index++) {
            Slot slot = menu.getSlot(index);
            if (!slot.hasItem()){
                Pattern icon = stationLayout.getSlot(index).getIcon();
                if (icon != null){
                    GuiUtil.renderPattern(
                            graphics,
                            icon,
                            cornerX + slot.x,
                            cornerY + slot.y
                    );
                }
            }
        }

        renderGuiEditMask(graphics);

        RenderSystem.enableDepthTest();
        super.renderBg(graphics, partialTick, mouseX, mouseY);

        renderReceptionContents(graphics);
        renderReadOnlyPageContents(graphics);
        renderReceptionScrollbar(graphics);
        renderArmorStand(graphics);
    }

    private void renderReadOnlyPageContents(GuiGraphics graphics) {
        if (leftPage != TranscendenceLeftPage.SUBLIMATION
            && leftPage != TranscendenceLeftPage.TUNING){
            return;
        }

        var content = leftLayout.content();
        int textY = content.y();
        Component message = Component.translatable(
                "gui.esotericism_tinker.transcendence_anvil.page.read_only"
        ).withStyle(ChatFormatting.DARK_GRAY);
        for (var line : font.split(message, content.width())) {
            graphics.drawString(font, line, content.x(), textY, 0xFF555555, false);
            textY += font.lineHeight;
        }
    }

    private void renderReceptionContents(GuiGraphics graphics) {
        if (leftPage == TranscendenceLeftPage.ROOT){
            return;
        }

        LazyToolStack pointSource = getShownTool();
        NoumenonData data = pointSource == null || pointSource.getStack().isEmpty()
                            ? null
                            : NoumenonData.read(pointSource.getTool());

        boolean usesElevation = leftPage == TranscendenceLeftPage.SUBLIMATION
                                || leftPage == TranscendenceLeftPage.INVESTITURE;
        Component pointName = Component.translatable(
                usesElevation
                ? "noumenon.esotericism_tinker.elevation"
                : "noumenon.esotericism_tinker.substrate"
        );
        int remainingPoints = data == null
                              ? 0
                              : usesElevation
                                ? data.remainingElevationPoints()
                                : data.remainingSubstratePoints();
        int totalPoints = data == null
                          ? 0
                          : usesElevation ? data.elevationPoints : data.substratePoints;

        var substrateBounds = leftLayout.substrate();
        int substrateY = substrateBounds.y();
        Component substrate = data == null
                              ? Component.translatable(
                "gui.esotericism_tinker.transcendence_anvil.substrate_unavailable",
                pointName
        )
                              : Component.translatable(
                "gui.esotericism_tinker.transcendence_anvil.substrate_points",
                pointName,
                remainingPoints,
                totalPoints
        );
        graphics.drawString(font, substrate, substrateBounds.x(), substrateY, 0xFFD7A900, false);

        if (leftPage != TranscendenceLeftPage.RECEPTION){
            return;
        }

        int end = Math.min(receptionTypes.size(),
                           receptionScroll + leftLayout.visibleReceptionRows());
        for (int index = receptionScroll; index < end; index++) {
            SlotType type = receptionTypes.get(index);
            int visibleRow = index - receptionScroll;
            var row = leftLayout.receptionRow(visibleRow);
            int textY = row.label().y()
                        + Math.max(1, (row.label().height() - font.lineHeight) / 2);

            int pending = menu.getPendingReception(type.getName());
            int cost = NoumenonDatabase.getReceptionCost(type.getName());
            Component line = Component.translatable(
                    "gui.esotericism_tinker.transcendence_anvil.reception_entry",
                    type.getDisplayName().copy()
                        .withStyle(style -> style.withColor(type.getColor())),
                    pending,
                    Component.translatable(
                            "gui.esotericism_tinker.transcendence_anvil.reception_cost",
                            cost
                    ).withStyle(ChatFormatting.DARK_GRAY)
            );

            graphics.drawString(font, line, row.label().x(), textY, 0xFFFFFFFF, false);
        }
    }

    private void renderGuiEditMask(GuiGraphics graphics) {
        if (!menu.hasPendingGuiChanges()){
            return;
        }

        for (int index = TranscendenceAnvilMenu.FIRST_MATERIAL_SLOT;
             index < TranscendenceAnvilMenu.FIRST_MATERIAL_SLOT
                     + TranscendenceAnvilMenu.MATERIAL_SLOT_COUNT;
             index++) {
            Slot slot = menu.getSlot(index);
            graphics.fill(
                    cornerX + slot.x,
                    cornerY + slot.y,
                    cornerX + slot.x + 16,
                    cornerY + slot.y + 16,
                    0x7F606060
            );
        }
    }

    private void rebuildInvestitureWidgets() {
        investitureTraits.clear();
        LazyToolStack source = menu.getOffhandTool();
        if (source == null || source.getStack().isEmpty()){
            return;
        }
        try {
            investitureTraits.addAll(
                    NoumenonInvestitureLogic.listDefinitionTraits(source.getTool())
            );
        }
        catch (RuntimeException ignored) {
            return;
        }

        clampInvestitureScroll();
        ResourceLocation selected = getSelectedInvestiture();
        int end = Math.min(
                investitureTraits.size(),
                investitureScroll + leftLayout.visibleReceptionRows()
        );
        for (int index = investitureScroll; index < end; index++) {
            ModifierEntry entry = investitureTraits.get(index);
            Component label = entry.getId().equals(selected)
                              ? entry.getDisplayName().copy()
                              : entry.getDisplayName().copy().withStyle(ChatFormatting.GRAY);
            int visibleIndex = index - investitureScroll;
            var row = leftLayout.receptionRow(visibleIndex).row();
            int traitIndex = index;
            Button button = addLeftButton(
                    label,
                    row.x(), row.y(), row.width(), row.height(),
                    ignored -> selectInvestiture(traitIndex)
            );
            button.active = canSelectInvestiture();
        }
    }

    private boolean canSelectInvestiture() {
        LazyToolStack input = menu.getInputTool();
        return input != null
               && !input.getStack().isEmpty()
               && NoumenonInvestitureLogic.canChangeInvestiture(
                NoumenonData.read(input.getTool())
        );
    }

    private void selectInvestiture(int traitIndex) {
        if (canSelectInvestiture()){
            EsotericismTinkerNetwork.CHANNEL.sendToServer(
                    new TranscendenceInvestiturePacket(menu.containerId, traitIndex)
            );
        }
    }

    private ResourceLocation getSelectedInvestiture() {
        LazyToolStack shown = getShownTool();
        if (shown == null || shown.getStack().isEmpty()){
            return null;
        }
        NoumenonData data = NoumenonData.read(shown.getTool());
        return data.investedTraits.keySet().stream().findFirst().orElse(null);
    }

    private void clampInvestitureScroll() {
        int maximum = Math.max(
                0,
                investitureTraits.size() - leftLayout.visibleReceptionRows()
        );
        investitureScroll = Math.max(0, Math.min(investitureScroll, maximum));
    }

    void renderReceptionScrollbar(GuiGraphics graphics) {
        if (leftPage != TranscendenceLeftPage.RECEPTION
            || receptionTypes.size() <= leftLayout.visibleReceptionRows()){
            return;
        }

        var scrollbar = leftLayout.scrollbar();
        int trackX = scrollbar.x();
        int trackTop = scrollbar.y();
        int trackHeight = scrollbar.height();
        graphics.fill(
                trackX, trackTop,
                trackX + scrollbar.width(), trackTop + trackHeight,
                0xFF303030
        );

        int thumbHeight = Math.max(
                10,
                trackHeight * leftLayout.visibleReceptionRows() / receptionTypes.size()
        );
        int maximumScroll =
                Math.max(1, receptionTypes.size() - leftLayout.visibleReceptionRows());
        int thumbTravel = trackHeight - thumbHeight;
        int thumbTop = trackTop + thumbTravel * receptionScroll / maximumScroll;
        graphics.fill(trackX, thumbTop, trackX + 2, thumbTop + thumbHeight, 0xFFC0C0C0);
    }

    @Override
    protected void drawContainerName(GuiGraphics graphics) {
        graphics.drawString(font, getTitle(), 8, 8, 4210752, false);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateDisplay();

        /*
         * DataSlot/recipe synchronization can change button validity without changing pages.
         */
        refreshReceptionButtons();
        syncInvestitureWidgets();
    }

    private void syncInvestitureWidgets() {
        if (leftPage != TranscendenceLeftPage.INVESTITURE){
            return;
        }
        LazyToolStack source = menu.getOffhandTool();
        ItemStack sourceStack = source == null ? ItemStack.EMPTY : source.getStack();
        ResourceLocation selected = getSelectedInvestiture();
        if (!ItemStack.isSameItemSameTags(sourceStack, lastInvestitureSource)
            || !java.util.Objects.equals(selected, lastSelectedInvestiture)){
            lastInvestitureSource = sourceStack.copy();
            lastSelectedInvestiture = selected;
            rebuildLeftWidgets();
            refreshLeftPanel();
        }
    }

    private record ReceptionButtons(SlotType type, Button minus, Button plus) {}
}
