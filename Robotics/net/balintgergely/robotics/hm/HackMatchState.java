package net.balintgergely.robotics.hm;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class HackMatchState{
	public static final int LEFT = 0,DOWN = 1,RIGHT = 2,UP = 3;
	public static final byte
		EMPTY = 0,
		GARBAGE = 1,
		TILE_LEAST = 2,
		TILE_R = 2,
		TILE_G = 3,
		TILE_B = 4,
		TILE_M = 5,
		TILE_Y = 6,
		SUPER_LEAST = 7,
		SUPER_R = 7,
		SUPER_G = 8,
		SUPER_B = 9,
		SUPER_M = 10,
		SUPER_Y = 11;
	public static final int
		FLIP_OFF = 1,
		GRAB_OFF = 1 + 6;
	public static final int WIDTH = 6, HEIGHT = 10;
	private static byte[][] cloneBoard(byte[][] board,int minChange){
		board = board.clone();
		for(int i = minChange;i < board.length;i++){
			board[i] = board[i].clone();
		}
		return board;
	}
	private static boolean isPowered(byte[][] board,int x,int y,int d,int r,byte kind){
		if(board[y][x] != kind){
			return false;
		}
		if(r == 0){
			return true;
		}
		for(int v = 0;v < 4;v++){
			if(v == d){
				continue;
			}
			int dx = x;
			int dy = y;
			switch(v){
				case LEFT: dx--; break;
				case UP: dy--; break;
				case RIGHT: dx++; break;
				case DOWN: dy++; break;
			}
			if(dx < 0 || dx >= 6){
				continue;
			}
			if(dy < 0 || dy >= board.length){
				continue;
			}
			if(isPowered(board, dx, dy, (v + 2) % 4, r - 1, kind)){
				return true;
			}
		}
		return false;
	}
	private final byte[][] board;
	private final byte hand;
	private final HackMatchState parent;
	private final int latestMove;
	private volatile int score = -1;
	public int getHeight(){
		return board.length;
	}
	public byte getAt(int x,int y){
		return board[y][x];
	}
	public HackMatchState getParent(){
		return parent;
	}
	public boolean hasLatestMove(){
		return latestMove != 0;
	}
	public boolean isLatestMoveSwap(){
		return latestMove < GRAB_OFF;
	}
	public int getLatestMoveX(){
		return (latestMove - 1) % 6;
	}
	private HackMatchState(byte[][] board,byte hand,HackMatchState parent,int lastMove){
		this.board = board;
		this.hand = hand;
		this.parent = parent;
		this.latestMove = lastMove;
		int y = board.length - 1;
		if(y >= 0){
			for(int x = 0;x < 6;x++){
				if(board[y][x] != EMPTY){
					return;
				}
			}
			throw new RuntimeException("Invariant violated!");
		}
	}
	public HackMatchState(){
		this(new byte[0][],EMPTY,null,0);
	}
	public boolean isHoldingTile(){
		return hand != EMPTY;
	}
	public HackMatchState addBlockRow(byte[] row){
		byte[][] newBoard = new byte[getHeight() + 1][];
		System.arraycopy(board, 0, newBoard, 1, getHeight());
		newBoard[0] = Arrays.copyOf(row, WIDTH);
		return new HackMatchState(newBoard,hand,null,0);
	}
	public HackMatchState addBlockRowBottom(byte[] row){
		byte[][] newBoard = new byte[getHeight() + 1][];
		System.arraycopy(board, 0, newBoard, 0, getHeight());
		newBoard[getHeight()] = Arrays.copyOf(row, WIDTH);
		return new HackMatchState(newBoard,hand,this,0);
	}
	public int getColumnHeight(int x){
		int y = board.length;
		while(y > 0){
			y--;
			if(board[y][x] != EMPTY){
				return y + 1;
			}
		}
		return 0;
	}
	public HackMatchState flip(int x){
		int h = getColumnHeight(x);
		int y0 = h - 1;
		int y1 = h - 2;
		if(h < 2){
			return this;
		}else{
			byte v0 = board[y0][x];
			byte v1 = board[y1][x];
			if(v0 == v1){
				return this;
			}
			byte[][] newBoard = board.clone();
			newBoard[y0] = newBoard[y0].clone();
			newBoard[y1] = newBoard[y1].clone();
			newBoard[y0][x] = v1;
			newBoard[y1][x] = v0;
			return new HackMatchState(newBoard,hand,this,FLIP_OFF + x);
		}
	}
	public HackMatchState move(int x,int y){
		if(x == y){
			return this;
		}
		HackMatchState s = grabOrDrop(x);
		if(s.hand != EMPTY){
			return s.grabOrDrop(y);
		}else{
			return s;
		}
	}
	public HackMatchState grabOrDrop(int x){
		int h = getColumnHeight(x);
		byte[][] newBoard;
		byte newHand;
		if(hand == EMPTY){
			if(h == 0){
				return this;
			}
			int y = h - 1;
			newHand = board[y][x];
			boolean doShrink = false;
			if(h == board.length){
				doShrink = true;
				for(int dx = 0;dx < 6;dx++){
					if(dx != x && board[y][dx] != EMPTY){
						doShrink = false;
						break;
					}
				}
			}
			if(doShrink){
				newBoard = Arrays.copyOf(board, y);
			}else{
				newBoard = board.clone();
				newBoard[y] = newBoard[y].clone();
				newBoard[y][x] = EMPTY;
			}
		}else{
			newHand = EMPTY;
			int y = h;
			if(y == board.length){
				newBoard = Arrays.copyOf(board, y + 1);
				newBoard[y] = new byte[6];
			}else{
				newBoard = Arrays.copyOf(board, board.length);
				newBoard[y] = newBoard[y].clone();
			}
			newBoard[y][x] = hand;
		}
		return new HackMatchState(newBoard, newHand, this, GRAB_OFF + x);
	}
	private static byte[][] dropUp(byte[][] board){
		for(int y = 0;y < board.length - 1;y++){
			for(int x = 0;x < 6;x++){
				if(board[y][x] == EMPTY && board[y + 1][x] != EMPTY){
					board[y][x] = board[y + 1][x];
					board[y + 1][x] = EMPTY;
				}
			}
		}
		int y = board.length;
		main: while(true){
			for(int x = 0;x < 6;x++){
				if(board[y - 1][x] != EMPTY){
					break main;
				}
			}
			y--;
		}
		if(y < board.length){
			return Arrays.copyOf(board, y);
		}else{
			return board;
		}
	}
	private void clear(byte[][] target,int x,int y,byte kind){
		if(target[y][x] != kind){
			return;
		}
		target[y][x] = EMPTY;
		for(int v = 0;v < 4;v++){
			int dx = x;
			int dy = y;
			switch(v){
				case LEFT: dx--; break;
				case UP: dy--; break;
				case RIGHT: dx++; break;
				case DOWN: dy++; break;
			}
			if(dx < 0 || dx >= 6){
				continue;
			}
			if(dy < 0 || dy >= getHeight()){
				continue;
			}
			clear(target, dx, dy, kind);
		}
	}
	private HackMatchState clearMatches(){
		byte[][] board = this.board;
		for(int y = 0;y < getHeight();y++){
			for(int x = 0;x < 6;x++){
				byte k = board[y][x];
				if(TILE_LEAST <= k && k <= SUPER_LEAST){
					if(isPowered(board, x, y, 5, 4, k)){
						if(board == this.board){
							board = cloneBoard(board, y);
						}
						clear(board, x, y, k);
					}
				}
			}
		}
		return this;
	}
	public int getScore(){
		int score = this.score;
		if(score == -1){
			score = 1000;
			for(int y = 0;y < getHeight();y++){
				for(int x = 0;x < 6;x++){
					byte kind = board[y][x];
					if(TILE_LEAST <= kind && kind < SUPER_LEAST){
						if(isPowered(board, x, y, 5, 4, kind)){
							score += 100;
						}
					}
					if(SUPER_LEAST <= kind){
						if(isPowered(board, x, y, 5, 2, kind)){
							score += 500;
						}
					}
				}
			}
			score -= getHeight();
			this.score = score;
		}
		return score;
	}
}
