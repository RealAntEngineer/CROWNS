package com.rae.crowns.content.fields.util;

import com.rae.formicapi.fondation.math.operators.CSRMatrix;
import com.rae.formicapi.fondation.math.operators.MutableMatrix;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;

/**
 * Mutable CSR matrix that supports efficient row updates.
 * Maintains both CSR format for fast multiplication and per-row maps for modification.
 */
public class MutableCSRMatrix implements MutableMatrix {
    private final int rows;
    private final int cols;

    // CSR storage (rebuilt when dirty)
    private double[] values;
    private int[] colIndex;
    private int[] rowPtr;

    // Mutable per-row storage
    private final Int2DoubleMap[] rowMaps;

    // Dirty flag - CSR needs rebuild
    private boolean dirty = true;

    public MutableCSRMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.rowMaps = new Int2DoubleMap[rows];
        for (int i = 0; i < rows; i++) {
            this.rowMaps[i] = new Int2DoubleOpenHashMap();
            this.rowMaps[i].defaultReturnValue(0.0);
        }
    }

    /**
     * Update an entire row efficiently.
     * Replaces all values in the row with new entries.
     */
    public void updateRow(int row, Int2DoubleMap newEntries) {
        if (row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("Row " + row + " out of bounds");
        }

        rowMaps[row].clear();
        rowMaps[row].putAll(newEntries);
        dirty = true;
    }

    /**
     * Update a single entry in a row.
     */
    @Override
    public void set(int row, int col, double value) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException();
        }

        if (value == 0.0) {
            rowMaps[row].remove(col);
        } else {
            rowMaps[row].put(col, value);
        }
        dirty = true;
    }

    @Override
    public void add(int row, int col, double value) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException();
        }

        if (value != 0.0) {
            rowMaps[row].put(col, rowMaps[row].get(col) + value);
            dirty = true;
        }
    }

    @Override
    public double get(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException();
        }
        return rowMaps[row].get(col);
    }

    /**
     * Rebuild CSR arrays from row maps if dirty.
     */
    private void rebuildCSR() {
        if (!dirty) return;

        // Count non-zeros
        int nnz = 0;
        for (Int2DoubleMap row : rowMaps) {
            nnz += row.size();
        }

        // Allocate arrays
        values = new double[nnz];
        colIndex = new int[nnz];
        rowPtr = new int[rows + 1];

        // Build CSR
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            rowPtr[r] = idx;

            // Sort column indices for this row
            IntList cols = new IntArrayList(rowMaps[r].keySet());
            cols.sort(null);

            for (int c : cols) {
                double val = rowMaps[r].get(c);
                if (val != 0.0) {
                    values[idx] = val;
                    colIndex[idx] = c;
                    idx++;
                }
            }
        }
        rowPtr[rows] = idx;

        dirty = false;
    }

    @Override
    public void multiply(double[] x, double[] result) {
        if (x.length != cols || result.length != rows) {
            throw new IllegalArgumentException("Dimension mismatch");
        }

        rebuildCSR();

        for (int r = 0; r < rows; r++) {
            double sum = 0.0;
            for (int k = rowPtr[r]; k < rowPtr[r + 1]; k++) {
                sum += values[k] * x[colIndex[k]];
            }
            result[r] = sum;
        }
    }

    @Override
    public void transposeMultiply(double[] x, double[] result) {
        if (x.length != rows || result.length != cols) {
            throw new IllegalArgumentException("Dimension mismatch");
        }

        rebuildCSR();

        Arrays.fill(result, 0.0);
        for (int r = 0; r < rows; r++) {
            for (int k = rowPtr[r]; k < rowPtr[r + 1]; k++) {
                result[colIndex[k]] += values[k] * x[r];
            }
        }
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public int cols() {
        return cols;
    }

    /**
     * Convert to immutable CSR matrix (creates a snapshot)
     */
    public CSRMatrix toCSR() {
        rebuildCSR();
        return new CSRMatrix(rows, cols,
                Arrays.copyOf(values, values.length),
                Arrays.copyOf(colIndex, colIndex.length),
                Arrays.copyOf(rowPtr, rowPtr.length)
        );
    }

    /**
     * Get number of non-zero entries
     */
    public int nnz() {
        rebuildCSR();
        return values.length;
    }

    /**
     * Check if matrix needs CSR rebuild
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Get a copy of the row map for inspection/modification
     */
    public Int2DoubleMap getRowCopy(int row) {
        return new Int2DoubleOpenHashMap(rowMaps[row]);
    }
}