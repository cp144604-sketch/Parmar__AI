package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NoteEntity
import com.example.data.local.ParmarDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: ParmarDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ParmarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches PARMAR AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PARMAR AI", appName)
    }

    @Test
    fun `room database persists local note and deletes cleanly`() = runTest {
        val noteId = db.noteDao().upsert(NoteEntity(title = "Gujarati Note", content = "હેલો Sir"))
        val allNotes = db.noteDao().observeAll().first()
        assertEquals(1, allNotes.size)
        assertEquals("Gujarati Note", allNotes.first().title)

        db.noteDao().deleteById(noteId)
        val afterDelete = db.noteDao().observeAll().first()
        assertEquals(0, afterDelete.size)
    }
}
