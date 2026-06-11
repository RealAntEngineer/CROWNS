package com.rae.crowns.content.fields.temperature;

import com.rae.formicapi.fondation.math.operators.MutableMatrix;

import java.util.Arrays;

/**
 * Mutable CSR matrix with a fixed sparsity structure.
 *
 * <p>This implementation assumes:
 * <ul>
 *     <li>The sparsity pattern is known at construction time</li>
 *     <li>No structural modifications (no insert/delete of non-zeros)</li>
 *     <li>Only numerical updates are allowed</li>
 * </ul>
 *
 * <p>All (row, col) pairs MUST already exist in the CSR structure.
 * If a non-existing entry is accessed or modified, the implementation
 * throws an {@link IllegalStateException}.
 *
 * <p>This design is optimized for:
 * <ul>
 *     <li>Finite element solvers (FEM)</li>
 *     <li>Iterative linear solvers (CG, GMRES, etc.)</li>
 *     <li>Large sparse systems with fixed connectivity</li>
 * </ul>
 */
public class PaddedCSRMatrix implements MutableMatrix {

    private final int rows;
    private final int cols;
    private final int nnzPerRow;

    /**
     * CSR values (mutable)
     */
    private final double[] values;

    /**
     * CSR column indices
     */
    private final int[] colIndex;

    /**
     * Constructs a fully defined CSR matrix.
     *
     * <p>The structure MUST already be consistent:
     * <ul>
     *     <li>{@code values.length == colIndex.length == nnz}</li>
     *     <li>{@code rowPtr.length == rows + 1}</li>
     *     <li>{@code rowPtr[rows] == nnz}</li>
     * </ul>
     *
     * <p>This constructor builds the fast lookup table used for updates.
     *
     * @param rows number of rows
     * @param cols number of columns
     *
     */
    public PaddedCSRMatrix(int rows, int cols, int nnzPerRow) {

        this.rows = rows;
        this.cols = cols;
        this.nnzPerRow = nnzPerRow;

        this.values = new double[rows * nnzPerRow];
        this.colIndex = new int[rows * nnzPerRow];
    }

    private PaddedCSRMatrix(int rows, int cols, int nnzPerRow, double[] values, int[] colIndex) {
        this.rows = rows;
        this.cols = cols;
        this.nnzPerRow = nnzPerRow;
        this.values = values;
        this.colIndex = colIndex;
    }

    /**
     * Returns a new {@link PaddedCSRMatrix} with {@code newRows} rows, sharing no
     * storage with this instance.
     *
     * <p>If {@code newRows < rows}: trailing rows are dropped — two {@link Arrays#copyOf}
     * calls, no per-row work.
     * <br>
     * If {@code newRows > rows}: new rows are zero-initialized (values) and their column
     * indices default to 0 — caller must populate them via {@link #setRow} before solving.
     *
     * @param newRows target row (and column) count
     * @return a resized copy
     */
    public PaddedCSRMatrix resize(int newRows) {
        int len = newRows * nnzPerRow;
        return new PaddedCSRMatrix(
                newRows, newRows, nnzPerRow,
                Arrays.copyOf(values, len),
                Arrays.copyOf(colIndex, len)
        );
    }

    /**
     * Adds a value to an existing entry in the CSR structure.
     *
     * <p>This method only works if the (row, col) entry already exists.
     * If it does not exist, an {@link IllegalStateException} is thrown.
     *
     * <p>Complexity: O(nnz_per_row) worst case (typically ~7).
     *
     * @param row   row index
     * @param col   column index
     * @param value value to add
     */
    @Override
    public void add(int row, int col, double value) {
        if (value == 0.0) return;

        int idx = findIndex(row, col);
        values[idx] += value;
    }

    /**
     * Sets a value in an existing CSR entry.
     *
     * <p>If the value is 0, the method simply writes 0 but does not remove
     * the entry (structure is fixed).
     *
     * <p>If the (row, col) entry does not exist, an exception is thrown.
     *
     * @param row   row index
     * @param col   column index
     * @param value new value
     */
    @Override
    public void set(int row, int col, double value) {
        int idx = findIndex(row, col);
        values[idx] = value;
    }

    /**
     * Finds CSR index for a (row, col) pair.
     *
     * @throws IllegalStateException if entry does not exist
     */
    private int findIndex(int row, int col) {
        int base = row * nnzPerRow;

        for (int i = 0; i < nnzPerRow; i++) {
            if (colIndex[base + i] == col) {
                return base + i;
            }
        }

        throw new IllegalStateException(
                "CSR entry does not exist: (" + row + "," + col + ")"
        );
    }

    /**
     * Overwrites an entire row of the matrix, including both its structure
     * (column indices) and its values.
     *
     * <p>This method assumes a fixed row capacity defined by {@code nnzPerRow}.
     * The provided {@code cols} array defines the column positions for each
     * entry in the row, and {@code newValues} defines the corresponding
     * numerical values.
     *
     * <p>After this call:
     * <ul>
     *     <li>The previous contents of the row are fully replaced</li>
     *     <li>The column structure of the row is updated to match {@code cols}</li>
     *     <li>No resizing of the underlying storage occurs</li>
     * </ul>
     *
     * <p><b>Important:</b> This operation modifies the sparsity structure of the row.
     * All subsequent operations (e.g., multiplication) will use the updated
     * column layout.
     *
     * <p>Constraints:
     * <ul>
     *     <li>{@code newValues.length == nnzPerRow}</li>
     *     <li>{@code cols.length == nnzPerRow}</li>
     * </ul>
     *
     * <p>Performance: O(nnzPerRow)
     *
     * @param row       row index to modify
     * @param newValues new non-zero values for the row
     * @param newCols   column indices corresponding to each value
     * @throws IllegalArgumentException if array sizes do not match {@code nnzPerRow}
     */
    public void setRow(int row, double[] newValues, int[] newCols, int count) {

        if (newValues.length > nnzPerRow || newCols.length != newValues.length) {
            throw new IllegalArgumentException(
                    "Expected arrays of size " + nnzPerRow +
                            " but got values=" + newValues.length +
                            " cols=" + newCols.length
            );
        }

        int base = row * nnzPerRow;
        for (int i = 0; i < count; i++) {
            colIndex[base + i] = newCols[i];
            values[base + i] = newValues[i];
        }
        // zero out trailing slots — both value AND colIndex set to safe default (diagonal)
        for (int i = count; i < nnzPerRow; i++) {
            colIndex[base + i] = row; // points to diagonal — safe for both multiply paths
            values[base + i] = 0.0;
        }
    }

    @Override
    public void multiply(double[] x, double[] result) {
        for (int r = 0; r < rows; r++) {

            int    base = r * nnzPerRow;
            double sum  = 0.0;

            for (int i = 0; i < nnzPerRow; i++) {
                sum += values[base + i] * x[colIndex[base + i]];
            }

            result[r] = sum;
        }
    }

    @Override
    public void transposeMultiply(double[] x, double[] result) {
        Arrays.fill(result, 0.0);

        for (int r = 0; r < rows; r++) {

            int base = r * nnzPerRow;

            for (int i = 0; i < nnzPerRow; i++) {
                result[colIndex[base + i]] += values[base + i] * x[r];
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

    @Override
    public double get(int r, int c) {
        int base = r * nnzPerRow;

        for (int i = 0; i < nnzPerRow; i++) {
            if (colIndex[base + i] == c) {
                return values[base + i];
            }
        }

        return 0.0;
    }

    public double[] getRowValues(int r) {
        int base = r * nnzPerRow;

        double[] out = new double[nnzPerRow];
        System.arraycopy(values, base, out, 0, nnzPerRow);

        return out;
    }

    public int[] getRowCols(int r) {
        int base = r * nnzPerRow;

        int[] out = new int[nnzPerRow];
        System.arraycopy(colIndex, base, out, 0, nnzPerRow);

        return out;
    }
}