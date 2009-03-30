package cz.romario.opensudoku.gui;

import java.util.Formatter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import cz.romario.opensudoku.db.SudokuDatabase;
import cz.romario.opensudoku.game.SudokuCell;
import cz.romario.opensudoku.game.SudokuGame;
import cz.romario.opensudoku.game.SudokuCellCollection.OnChangeListener;
import cz.romario.opensudoku.gui.SelectNumberDialog.OnNumberSelectListener;
import cz.romario.opensudoku.gui.SudokuBoardView.OnCellTapListener;

/*
 * TODO:
 * - timer
 * - notes
 * - folder view (v detailu 10 puzzles / 3 solved)
 * - sudoku list (v detailu stav, cas a tak)
 * - select number dialog
 * 
 */

//TODO: vyresit proc tuhne, kdyz vytahnu klavesnici
public class SudokuPlayActivity extends Activity{
	
	private static final String TAG = "SudokuPlayActivity";
	
	public static final String EXTRAS_SUDOKU_ID = "sudoku_id";
	
	private static final int REQUEST_SELECT_NUMBER = 1;
	
	private static final int DIALOG_SELECT_NUMBER = 1;
	private static final int DIALOG_WELL_DONE = 2;
	
	private long sudokuGameID;
	private SudokuGame sudokuGame;
	
	private SelectNumberDialog selectNumberDialog;
	private SudokuBoardView sudokuBoard;
	private TextView timeLabel;
	
	private StringBuilder timeText;
	private Formatter timeFormatter;
	
	private GameTimer gameTimer;

	//PowerManager.WakeLock wakeLock;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sudoku_play);
        
        Button buttonLeave = (Button) findViewById(R.id.button_leave);
        buttonLeave.setOnClickListener(buttonLeaveClickListener);
        sudokuBoard = (SudokuBoardView)findViewById(R.id.sudoku_board);
        selectNumberDialog = new SelectNumberDialog(this);
        selectNumberDialog.setOnNumberSelectListener(onNumberSelectListener);
        timeLabel = (TextView)findViewById(R.id.time_label);
        timeText = new StringBuilder(5);
        timeFormatter = new Formatter(timeText);
        gameTimer = new GameTimer();
        
        // create sudoku game instance
        if (savedInstanceState == null) {
        	// activity runs for the first time, read game from database
        	sudokuGameID = getIntent().getLongExtra(EXTRAS_SUDOKU_ID, 0);
        	SudokuDatabase sudokuDB = new SudokuDatabase(this);
        	sudokuGame = sudokuDB.getSudoku(sudokuGameID);
        	//gameTimer.setTime(sudokuGame.getTime());
        } else {
        	// activity has been running before, restore its state
        	sudokuGame = (SudokuGame)savedInstanceState.getParcelable("sudoku_game");
        	gameTimer.restoreState(savedInstanceState);
        }
        
        if (sudokuGame.getState() == SudokuGame.GAME_STATE_NOT_STARTED) {
        	sudokuGame.setState(SudokuGame.GAME_STATE_PLAYING);
        }
        
        if (sudokuGame.getState() == SudokuGame.GAME_STATE_COMPLETED) {
        	updateTimeLabel(sudokuGame.getTime());
        }
        
        if (sudokuGame.getState() == SudokuGame.GAME_STATE_PLAYING) {
        	sudokuGame.start();
        }
        
    	setTitle(sudokuGame.getName());

    	//PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	// TODO
    	//wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
    	//wakeLock.acquire(5 * 60 * 1000);
        
        sudokuBoard.setCells(sudokuGame.getCells());
        sudokuBoard.setOnCellTapListener(cellTapListener);
        
		sudokuGame.getCells().addOnChangeListener(cellsOnChangeListener);
    }	
	
	private OnClickListener buttonLeaveClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			//sudokuGame.setTime(gameTimer.getTime());
			sudokuGame.pause();
			SudokuDatabase sudokuDB = new SudokuDatabase(SudokuPlayActivity.this);
			sudokuDB.updateSudoku(sudokuGame);
			finish();
		}
	};
    
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		if (sudokuGame.getState() == SudokuGame.GAME_STATE_PLAYING) {
			sudokuGame.start();
			gameTimer.start();
		}
	}
	
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	//if (wakeLock.isHeld()) {
    	//	wakeLock.release();
    	//}
		if (sudokuGame.getState() == SudokuGame.GAME_STATE_PLAYING) {
			sudokuGame.pause();
		}
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	// TODO Auto-generated method stub
    	super.onSaveInstanceState(outState);
		
    	// TODO: doresit timer, poradne retestnout, funguje divne
    	gameTimer.stop();
    	outState.putParcelable("sudoku_game", sudokuGame);
    	gameTimer.saveState(outState);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id){
    	case DIALOG_SELECT_NUMBER:
    		return selectNumberDialog.getDialog();
    	case DIALOG_WELL_DONE:
            return new AlertDialog.Builder(SudokuPlayActivity.this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Well Done!")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    /* User clicked OK so do some stuff */
                }
            })
            .create();
    	}
    	return null;
    }
    
    
    private OnCellTapListener cellTapListener = new OnCellTapListener() {

		@Override
		public boolean onCellTap(SudokuCell cell) {
			if (cell != null && cell.getEditable()) {
				showDialog(DIALOG_SELECT_NUMBER);
			}
			return true;
		}
    	
    };
    
	/**
	 * Occurs when number is selected in SelectNumberDialog.
	 */
    private OnNumberSelectListener onNumberSelectListener = new OnNumberSelectListener() {
		@Override
		public boolean onNumberSelect(int number) {
    		SudokuCell selectedCell = sudokuBoard.getSelectedCell();
    		if (number != -1) {
                // set cell number selected by user
				sudokuGame.getCells().setValue(selectedCell, number);
    		}
			return true;
		}
	};
    
	/**
	 * Occurs when any value in sudoku's cells changes.
	 */
	private OnChangeListener cellsOnChangeListener = new OnChangeListener() {
		@Override
		public boolean onChange() {
			sudokuGame.validate();
			
            // check whether game is completed, if so, finish the game
			if (sudokuGame.isCompleted()) {
                showDialog(DIALOG_WELL_DONE);
                sudokuGame.setState(SudokuGame.GAME_STATE_COMPLETED);
                sudokuBoard.setReadOnly(true);
            }
                
            // update board view
			sudokuBoard.postInvalidate();
			return true;
		}
	};

    
	/**
     * Update the status line to the current game state.
     */
	void updateStatus() {
		// Use StringBuilders and a Formatter to avoid allocating new
		// String objects every time -- this function is called often!
		
		//long time = gameTimer.getTime();
		long time = sudokuGame.getTime();
		updateTimeLabel(time);
	}
	
	void updateTimeLabel(long time) {
		timeText.setLength(0);
		timeFormatter.format("%02d:%02d", time / 60000, time / 1000 % 60);
		timeLabel.setText(timeText);
	}
    
    
	// This class implements the game clock.  All it does is update the
    // status each tick.
	private final class GameTimer extends Timer {
		
		GameTimer() {
    		super(1000);
    	}
		
    	@Override
		protected boolean step(int count, long time) {
    		updateStatus();
            
            // Run until explicitly stopped.
            return false;
        }
        
	}
	
}
