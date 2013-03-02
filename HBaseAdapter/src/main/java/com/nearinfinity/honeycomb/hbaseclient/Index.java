package com.nearinfinity.honeycomb.hbaseclient;

import com.google.common.base.Function;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class Index {
    /**
     * Convert column names into the correct HBase index row format.
     * For example:
     * column "A" has value 1
     * createColumnIds(["A"], {"A" => 1}) = [0, 0, 0, 0, 0, 0, 0, 1]
     *
     * @param columns        Column names to convert
     * @param columnNameToId Column names' map to numeric value
     * @return HBase index row format of the column names
     */
    public static byte[] createColumnIds(final Iterable<String> columns, final Map<String, Long> columnNameToId) {
        return correctColumnIdSize(convertToByteArray(columns, new Function<String, byte[]>() {
            @Override
            public byte[] apply(String column) {
                return Bytes.toBytes(columnNameToId.get(column));
            }
        }));
    }

    /**
     * Convert MySQL row into a HBase row format for given column names.
     *
     * @param columns Column names to convert
     * @param values  MySQL row values
     * @return HBase index row format of the column values
     */
    public static byte[] convertToHBaseFormat(final Iterable<String> columns, final Map<String, byte[]> values) {
        return convertToByteArray(columns, new Function<String, byte[]>() {
            @Override
            public byte[] apply(String column) {
                return values.get(column);
            }
        });
    }

    /**
     * Calculates the total length of the HBase index row given the columns.
     *
     * @param columns         Columns expected in the index
     * @param columnLengthMap Map of column to length
     * @return Total length
     */
    public static int calculateIndexValuesFullLength(final Iterable<String> columns, final Map<String, Integer> columnLengthMap) {
        int size = 0;
        for (String column : columns) {
            size += columnLengthMap.get(column);
        }

        return size;
    }

    /**
     * Retrieve the columns of the indexes.
     *
     * @param tableMetadata SQL table metadata
     * @return Columns in an index
     */
    public static List<List<String>> indexForTable(final Map<String, byte[]> tableMetadata) {
        return extractTableMetadata(tableMetadata, Constants.INDEXES);
    }

    /**
     * Retrieve the columns of the indexes with a uniqueness constraint.
     *
     * @param tableMetadata SQL table metadata
     * @return Columns with uniqueness constraint
     */
    public static List<List<String>> uniqueKeysForTable(final Map<String, byte[]> tableMetadata) {
        return extractTableMetadata(tableMetadata, Constants.UNIQUES);
    }

    /**
     * Creates the reverse index for a SQL row.
     *
     * @param tableId          Unique identifier of the SQL table
     * @param rowId            Unique identifier for the SQL row
     * @param descendingValues HBase encoded form of the SQL row
     * @param columns          Columns in the index
     * @param columnIds        HBase format of the columns
     * @return HBase index bytes for SQL row
     */
    public static byte[] createReverseIndex(long tableId, UUID rowId, Map<String, byte[]> descendingValues, List<String> columns, byte[] columnIds) {
        final byte[] descendingIndexValues = convertToHBaseFormat(columns, descendingValues);
        return RowKeyFactory.buildReverseIndexRowKey(tableId, columnIds, descendingIndexValues, rowId);
    }

    /**
     * Creates the primary index for a SQL row.
     *
     * @param tableId         Unique identifier of the SQL table
     * @param rowId           Unique identifier for the SQL row
     * @param ascendingValues HBase encoded form of the SQL row
     * @param columns         Columns in the index
     * @param columnIds       HBase format of the columns
     * @return HBase index bytes for SQL row
     */
    public static byte[] createPrimaryIndex(long tableId, UUID rowId, Map<String, byte[]> ascendingValues, List<String> columns, byte[] columnIds) {
        final byte[] ascendingIndexValues = convertToHBaseFormat(columns, ascendingValues);
        return RowKeyFactory.buildIndexRowKey(tableId, columnIds, ascendingIndexValues, rowId);
    }

    private static byte[] correctColumnIdSize(final byte[] columnIds) {
        int expectedSize = Constants.KEY_PART_COUNT * Bytes.SIZEOF_LONG;
        checkState(columnIds.length <= expectedSize, format("There should never be more than %d columns indexed. Found %d columns.", expectedSize / Bytes.SIZEOF_LONG, columnIds.length / Bytes.SIZEOF_LONG));

        if (columnIds.length == expectedSize) {
            return columnIds;
        }

        return Bytes.padTail(columnIds, expectedSize - columnIds.length);
    }

    private static byte[] convertToByteArray(final Iterable<String> columns,
                                             final Function<String, byte[]> conversion) {
        List<byte[]> pieces = new LinkedList<byte[]>();
        int size = 0;
        for (final String column : columns) {
            byte[] bytes = conversion.apply(column);
            if (bytes != null) {
                size += bytes.length;
                pieces.add(bytes);
            }
        }

        return Util.mergeByteArrays(pieces, size);
    }

    private static List<List<String>> extractTableMetadata(Map<String, byte[]> tableMetadata, byte[] key) {
        byte[] jsonBytes = null;
        for (Map.Entry<String, byte[]> entry : tableMetadata.entrySet()) {
            if (Arrays.equals(entry.getKey().getBytes(), key)) {
                jsonBytes = entry.getValue();
            }
        }

        if (jsonBytes == null) {
            return new LinkedList<List<String>>();
        }

        return Util.deserializeList(jsonBytes);
    }
}
