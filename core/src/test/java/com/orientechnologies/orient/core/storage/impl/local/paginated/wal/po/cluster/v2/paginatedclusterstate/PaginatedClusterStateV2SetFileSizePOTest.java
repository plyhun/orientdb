package com.orientechnologies.orient.core.storage.impl.local.paginated.wal.po.cluster.v2.paginatedclusterstate;

import com.orientechnologies.common.directmemory.OByteBufferPool;
import com.orientechnologies.common.directmemory.OPointer;
import com.orientechnologies.orient.core.storage.cache.OCacheEntry;
import com.orientechnologies.orient.core.storage.cache.OCacheEntryImpl;
import com.orientechnologies.orient.core.storage.cache.OCachePointer;
import com.orientechnologies.orient.core.storage.cluster.OClusterPage;
import com.orientechnologies.orient.core.storage.cluster.v2.OPaginatedClusterStateV2;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.po.PageOperationRecord;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class PaginatedClusterStateV2SetFileSizePOTest {
  @Test
  public void testRedo() {
    final int pageSize = OClusterPage.PAGE_SIZE;
    final OByteBufferPool byteBufferPool = new OByteBufferPool(pageSize);
    try {
      final OPointer pointer = byteBufferPool.acquireDirect(false);
      final OCachePointer cachePointer = new OCachePointer(pointer, byteBufferPool, 0, 0);
      final OCacheEntry entry = new OCacheEntryImpl(0, 0, cachePointer, false);

      OPaginatedClusterStateV2 clusterState = new OPaginatedClusterStateV2(entry);
      clusterState.setFileSize(12);

      entry.clearPageOperations();

      final OPointer restoredPointer = byteBufferPool.acquireDirect(false);
      final OCachePointer restoredCachePointer =
          new OCachePointer(restoredPointer, byteBufferPool, 0, 0);
      final OCacheEntry restoredCacheEntry = new OCacheEntryImpl(0, 0, restoredCachePointer, false);

      final ByteBuffer originalBuffer = cachePointer.getBufferDuplicate();
      final ByteBuffer restoredBuffer = restoredCachePointer.getBufferDuplicate();

      Assert.assertNotNull(originalBuffer);
      Assert.assertNotNull(restoredBuffer);

      restoredBuffer.put(originalBuffer);

      clusterState.setFileSize(42);

      final List<PageOperationRecord> operations = entry.getPageOperations();
      Assert.assertEquals(1, operations.size());

      Assert.assertTrue(operations.get(0) instanceof PaginatedClusterStateV2SetFileSizePO);
      final PaginatedClusterStateV2SetFileSizePO pageOperation =
          (PaginatedClusterStateV2SetFileSizePO) operations.get(0);

      OPaginatedClusterStateV2 restoredPage = new OPaginatedClusterStateV2(restoredCacheEntry);
      Assert.assertEquals(12, restoredPage.getFileSize());

      pageOperation.redo(restoredCacheEntry);

      Assert.assertEquals(42, restoredPage.getFileSize());

      byteBufferPool.release(pointer);
      byteBufferPool.release(restoredPointer);
    } finally {
      byteBufferPool.clear();
    }
  }

  @Test
  public void testUndo() {
    final int pageSize = OClusterPage.PAGE_SIZE;

    final OByteBufferPool byteBufferPool = new OByteBufferPool(pageSize);
    try {
      final OPointer pointer = byteBufferPool.acquireDirect(false);
      final OCachePointer cachePointer = new OCachePointer(pointer, byteBufferPool, 0, 0);
      final OCacheEntry entry = new OCacheEntryImpl(0, 0, cachePointer, false);

      OPaginatedClusterStateV2 clusterState = new OPaginatedClusterStateV2(entry);
      clusterState.setFileSize(12);

      entry.clearPageOperations();

      clusterState.setFileSize(42);

      final List<PageOperationRecord> operations = entry.getPageOperations();
      Assert.assertEquals(1, operations.size());

      Assert.assertTrue(operations.get(0) instanceof PaginatedClusterStateV2SetFileSizePO);

      final PaginatedClusterStateV2SetFileSizePO pageOperation =
          (PaginatedClusterStateV2SetFileSizePO) operations.get(0);

      final OPaginatedClusterStateV2 restoredPage = new OPaginatedClusterStateV2(entry);
      Assert.assertEquals(42, restoredPage.getFileSize());

      pageOperation.undo(entry);

      Assert.assertEquals(12, restoredPage.getFileSize());

      byteBufferPool.release(pointer);
    } finally {
      byteBufferPool.clear();
    }
  }

  @Test
  public void testSerialization() {
    PaginatedClusterStateV2SetFileSizePO operation =
        new PaginatedClusterStateV2SetFileSizePO(12, 42);

    operation.setFileId(42);
    operation.setPageIndex(24);
    operation.setOperationUnitId(1);

    final int serializedSize = operation.serializedSize();
    final byte[] stream = new byte[serializedSize + 1];
    int pos = operation.toStream(stream, 1);

    Assert.assertEquals(serializedSize + 1, pos);

    PaginatedClusterStateV2SetFileSizePO restoredOperation =
        new PaginatedClusterStateV2SetFileSizePO();
    restoredOperation.fromStream(stream, 1);

    Assert.assertEquals(42, restoredOperation.getFileId());
    Assert.assertEquals(24, restoredOperation.getPageIndex());
    Assert.assertEquals(1, restoredOperation.getOperationUnitId());

    Assert.assertEquals(12, restoredOperation.getOldFileSize());
    Assert.assertEquals(42, restoredOperation.getNewFileSize());
  }
}
