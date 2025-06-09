package com.mbougar.swapware.ui.chat

import com.google.common.truth.Truth.assertThat
import com.mbougar.swapware.viewmodel.ChatListItem
import com.mbougar.swapware.viewmodel.formatDateSeparator
import com.mbougar.swapware.viewmodel.groupMessagesWithDateSeparators
import com.mbougar.swapware.data.model.Message
import org.junit.Test
import java.util.Calendar
import java.util.Date

class ChatDateUtilsTest {

    private fun createDate(daysAgo: Int, hour: Int, minute: Int): Date {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }.time
    }

    @Test
    fun `groupMessagesWithDateSeparators groups messages correctly`() {
        val messages = listOf(
            Message(id = "1", timestamp = createDate(2, 10, 30)),
            Message(id = "2", timestamp = createDate(1, 11, 0)),
            Message(id = "3", timestamp = createDate(1, 12, 0)),
            Message(id = "4", timestamp = createDate(0, 9, 15))
        )

        val chatListItems = groupMessagesWithDateSeparators(messages)

        assertThat(chatListItems).hasSize(7)
        assertThat(chatListItems[0]).isInstanceOf(ChatListItem.DateSeparatorItem::class.java)
        assertThat(chatListItems[1]).isInstanceOf(ChatListItem.MessageItem::class.java)
        assertThat(chatListItems[2]).isInstanceOf(ChatListItem.DateSeparatorItem::class.java)
        assertThat(chatListItems[3]).isInstanceOf(ChatListItem.MessageItem::class.java)
        assertThat(chatListItems[4]).isInstanceOf(ChatListItem.MessageItem::class.java)
        assertThat(chatListItems[5]).isInstanceOf(ChatListItem.DateSeparatorItem::class.java)
    }

    @Test
    fun `formatDateSeparator returns 'Today' for current date`() {
        val result = formatDateSeparator(createDate(0, 1, 1))
        assertThat(result).isEqualTo("Today")
    }

    @Test
    fun `formatDateSeparator returns 'Yesterday' for previous date`() {
        val result = formatDateSeparator(createDate(1, 1, 1))
        assertThat(result).isEqualTo("Yesterday")
    }
}
