package com.gerbort.database.dao

import androidx.room.*
import com.gerbort.database.model.TreeNodeEntity

@Dao
interface TreeNodeDao {

    @Query("SELECT * FROM TreeNodeEntity")
    fun getNodes(): List<TreeNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNodes(nodes: List<TreeNodeEntity>)

    @Delete
    fun deleteNodes(nodes: List<TreeNodeEntity>)

    @Query("DELETE FROM TreeNodeEntity WHERE id IN (:nodesId)")
    fun deleteNodesById(nodesId: List<Int>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateNodes(nodes: List<TreeNodeEntity>)

    @Query("DELETE FROM TreeNodeEntity")
    fun clearNodes()

}