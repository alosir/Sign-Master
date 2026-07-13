package com.alosir.task

import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.data.entity.CheckinCycleType
import com.alosir.task.util.CycleCalculator
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class CheckinItemTest {
    
    @Test
    fun checkinItem_creation() {
        val item = CheckinItem(
            name = "Test App",
            type = CheckinType.APP,
            packageName = "com.test.app",
            iconPath = ""
        )
        
        assertEquals("Test App", item.name)
        assertEquals(CheckinType.APP, item.type)
        assertEquals("com.test.app", item.packageName)
        assertNotNull(item.createdAt)
    }
    
    @Test
    fun checkinType_values() {
        assertEquals(0, CheckinType.APP)
        assertEquals(1, CheckinType.WEBSITE)
        assertEquals(2, CheckinType.OTHER)
    }
    
    @Test
    fun checkinCycleType_values() {
        assertEquals(0, CheckinCycleType.DAY)
        assertEquals(1, CheckinCycleType.WEEK)
        assertEquals(2, CheckinCycleType.MONTH)
    }
    
    @Test
    fun checkinItem_defaultCycleValues() {
        val item = CheckinItem(
            name = "Daily Task",
            type = CheckinType.OTHER,
            iconPath = ""
        )
        
        assertEquals(CheckinCycleType.DAY, item.cycleType)
        assertEquals(1, item.cycleValue)
    }
    
    @Test
    fun checkinItem_customCycleValues() {
        val item = CheckinItem(
            name = "Weekly Task",
            type = CheckinType.OTHER,
            iconPath = "",
            cycleType = CheckinCycleType.WEEK,
            cycleValue = 2
        )
        
        assertEquals(CheckinCycleType.WEEK, item.cycleType)
        assertEquals(2, item.cycleValue)
    }
    
    @Test
    fun cycleCalculator_dayCycle_notChecked() {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        val lastDate = formatter.format(yesterday.time)
        
        val status = CycleCalculator.getCheckinStatus(CheckinCycleType.DAY, 1, lastDate)
        assertEquals(com.alosir.task.util.CheckinStatus.PENDING, status)
    }
    
    @Test
    fun cycleCalculator_dayCycle_alreadyChecked() {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = formatter.format(Date())
        
        val status = CycleCalculator.getCheckinStatus(CheckinCycleType.DAY, 1, today)
        assertEquals(com.alosir.task.util.CheckinStatus.COMPLETED, status)
    }
}
