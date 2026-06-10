package org.brahypno.esotericismtinker.common.Items;

import net.minecraft.world.item.ItemStack;
import org.brahypno.esotericismtinker.library.client.book.EsotericismBook;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.client.book.BookScreenOpener;
import slimeknights.mantle.item.AbstractBookItem;

public class EsotericismBookItem extends AbstractBookItem {
    private final BookType bookType;

    public EsotericismBookItem(Properties props, BookType bookType) {
        super(props);
        this.bookType = bookType;
    }

    @Override
    public @NotNull BookScreenOpener getBook(@NotNull ItemStack stack) {
        return EsotericismBook.getBook(bookType);
    }

    /**
     * Simple enum to allow selecting the book on the client
     */
    public enum BookType {
        HYPNAGOGIC_TRANSMUTE
    }
}
