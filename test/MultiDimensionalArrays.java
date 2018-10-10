import static intr.Intrinsics.*;

public class MultiDimensionalArrays {
	public void completeAllocation() {
		final int a[][] = new int[4][4];
		assertEqual(a.length, 4);
		for(int i = 0; i < a.length; i++) {
			assertEqual(a[i].length, 4);
			for(int j = 0; j < a[i].length; j++) {
				a[i][j] = i * 7 + j * 3;
			}
		}
		
		for(int i = 0; i < a.length; i++) {
			for(int j = 0; j < a[i].length; j++) {
				assertEqual(a[i][j], i * 7 + j * 3);
			}
		}
	}
	
	public void partialSingleDimAllocation() {
		final int a[][] = new int[4][];
		assertEqual(a.length,4);
		for(int i = 0; i < a.length; i++) {
			assertEqual(a[i], null);
		}
		for(int i = 0; i < a.length; i++) {
			a[i] = new int[i+1];
			for(int j = 0; j < a[i].length; j++) {
				a[i][j] = i * 7 + j * 3;
			}
		}
		int numAssertions = 0;
		for(int i = 0; i < a.length; i++) {
			for(int j = 0; j < a[i].length; j++) {
				numAssertions++;
				assertEqual(a[i][j], i * 7 + j * 3);
			}
		}
		assertEqual(numAssertions, 1 + 2 + 3 + 4);
	}
	
	public void partialMultiDimAllocation() {
		final int a[][][] = new int[4][5][];
		assertEqual(a.length,4);
		for(int i = 0; i < a.length; i++) {
			assertNotEqual(a[i], null);
		}
		for(int i = 0; i < a.length; i++) {
			assertEqual(a[i].length, 5);
			for(int j = 0; j < a[i].length; j++) {
				a[i][j] = new int[(i+1) * 2];
				for(int k = 0; k < a[i][j].length; k++) {
					a[i][j][k] = i * 3 + j * 7 + 11 * k;
				}
			}
		}
		
		int numAssertions = 0;
		for(int i = 0; i < a.length; i++) {
			for(int j = 0; j < a[i].length; j++) {
				assertEqual(a[i][j].length, (i+1) * 2);
				for(int k = 0; k < a[i][j].length; k++) {
					numAssertions++;
					assertEqual(a[i][j][k], i * 3 + j * 7 + 11 * k);
				}
			}
		}
		final int expectedAssertions = (5 * 2) + (5 * 4) + (5 * 6) + (5 * 8);
		assertEqual(numAssertions, expectedAssertions);
	}
	
	public void completeNondetAllocation() {
		final int[][] a = new int[nondet()][nondet()];
		a[0][3] = 4;
		assertEqual(a[3][4], lift(4, 0));
		assertEqual(a.length, nondet());
		assertEqual(a[0].length, nondet());
		assertNotEqual(a[1] == a[2], true);
		assertNotEqual(a[1] == a[2], false);
	}
	
	public void mixedAllocation() {
		final int[][] a = new int[4][nondet()];
		assertEqual(a.length, 4);
		for(int i = 0; i < a.length; i++) {
			assertEqual(a[i].length, nondet());
		}
		a[1][0] = 3;
		assertEqual(a[1][2], lift(0, 3));
		assertEqual(a[0][0], 0);
		a[1] = new int[4];
		assertEqual(a[1].length, 4);
		for(int i = 0; i < a[1].length; i++) {
			a[1][i] = i;
		}
		for(int i = 0; i < a[1].length; i++) {
			assertEqual(a[1][i], i);
		}
		final int[] vals = new int[10];
		for(int i = 0; i < 10; i++) {
			a[0][i] = i;
			vals[i] = i;
		}
		for(int i = 0; i < 10; i++) {
			assertEqual(a[0][i], lift(vals));
		}
	}
	
	public void partialNondetAllocation() {
		final int[][][] a = new int[nondet()][nondet()][];
		assertEqual(a[0][0], null);
		final int[][] sideEffect = new int[10][];
		for(int i = 0; i < 10; i++) {
			final int[] alloc = new int[i+1];
			sideEffect[i] = alloc;
			a[i][i] = alloc;
		}
		assertNotEqual(a[nondet()][nondet()], null);
		assertNotEqual(a[0][0], null);
		sideEffect[4][3] = 10;
		assertEqual(sideEffect[4][3], 10);
		assertEqual(sideEffect[4][0], 0);
		assertEqual(a[nondet()][nondet()][3], lift(0, 10));
		assertEqual(a[nondet()][nondet()][2], 0);
		a[nondet()][nondet()][4] = 11;
		for(int i = 4; i < 10; i++) {
			assertEqual(sideEffect[i][4], lift(0, 11));
		}
	}
}
