package com.yalematta.battleship.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.yalematta.battleship.R
import com.yalematta.battleship.data.models.Board
import com.yalematta.battleship.data.models.Ship
import com.yalematta.battleship.internal.getViewModel
import com.yalematta.battleship.ui.game.GameActivity
import com.yalematta.battleship.ui.main.MainActivity.Companion.ME_PLAYER
import com.yalematta.battleship.ui.main.MainActivity.Companion.ROLE_NAME
import com.yalematta.battleship.ui.main.MainActivity.Companion.ROOM_NAME
import com.yalematta.battleship.ui.main.MainActivity.Companion.VS_PLAYER
import com.yalematta.battleship.ui.setup.adapter.BoardGridAdapter
import com.yalematta.battleship.ui.setup.adapter.ShipListAdapter
import kotlinx.android.synthetic.main.activity_setup.*
import kotlinx.coroutines.*

class SetupActivity : AppCompatActivity(), Animation.AnimationListener {

    companion object {
        const val BOARD = "BOARD"
        const val FLEET = "FLEET"
    }

    private lateinit var shipAdapter: ShipListAdapter
    private lateinit var boardAdapter: BoardGridAdapter

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private val viewModel by lazy {
        getViewModel { SetupViewModel() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        title = getString(R.string.place_ships)

        initObservers()
        initBoardAdapter()
        initShipAdapter()

        viewModel.roomName = intent.getStringExtra(ROOM_NAME)
        viewModel.roleName = intent.getStringExtra(ROLE_NAME)

        viewModel.myPlayer = intent.getParcelableExtra(ME_PLAYER)
        viewModel.vsPlayer = intent.getParcelableExtra(VS_PLAYER)

        randomButton.setOnClickListener {
            viewModel.generateRandomShips()
            startButton.visibility = View.VISIBLE
            viewModel.startGameVisibility = true
        }

        manualButton.setOnClickListener {
            viewModel.initShips()
            randomButton.visibility = View.GONE;
            manualButton.visibility = View.GONE;
            viewModel.shipListVisibility = true
            updateVisibility()
        }

        rotateButton.setOnClickListener {
            viewModel.rotateShip()
        }

        startButton.setOnClickListener {

            val bundle = Bundle()
            bundle.putString(ROOM_NAME, viewModel.roomName)
            bundle.putString(ROLE_NAME, viewModel.roleName)
            bundle.putParcelable(ME_PLAYER, viewModel.myPlayer)
            bundle.putParcelable(VS_PLAYER, viewModel.vsPlayer)
            bundle.putSerializable(BOARD, viewModel.board.fieldStatus)
            bundle.putParcelableArrayList(FLEET, viewModel.board.fleet)

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtras(bundle)
            this.startActivity(intent)
            finish()
        }

    }

    private fun initObservers() {
        viewModel.apply {
            refreshBoardLiveData.observe(this@SetupActivity,
                Observer { board -> refreshBoard(board) })
            shipsLiveData.observe(this@SetupActivity,
                Observer { ships -> addDataToShipAdapter(ships) })
            blinkLiveData.observe(this@SetupActivity,
                Observer { view -> setBlinkAnimation(view) })
            refreshShipsLiveData.observe(this@SetupActivity,
                Observer { shipList -> refreshShips(shipList) })
        }
    }

    override fun onResume() {
        super.onResume()
        updateVisibility()
    }

    private fun updateVisibility() {
        if (!viewModel.startGameVisibility) {
            shipsLayout.visibility = if (viewModel.shipListVisibility) View.VISIBLE else View.GONE
            randomButton.visibility = if (viewModel.shipListVisibility) View.GONE else View.VISIBLE
            manualButton.visibility = if (viewModel.shipListVisibility) View.GONE else View.VISIBLE
        } else {
            startButton.visibility = View.VISIBLE
        }
    }

    private fun refreshBoard(board: Board) {
        boardAdapter.refresh(board.fieldStatus)
        randomButton.visibility = View.GONE;
        manualButton.visibility = View.GONE;
    }

    private fun initBoardAdapter() {
        boardAdapter =
            BoardGridAdapter(
                this,
                viewModel.board.fieldStatus
            )
            { view: View, position: Int -> handleBoardClick(view, position) }

        boardGridView.adapter = boardAdapter
    }


    private fun addDataToShipAdapter(ships: ArrayList<Ship>) {
        shipAdapter.refreshShipList(ships)
    }

    private fun refreshShips(shipList: ArrayList<Ship>) {
        shipAdapter.selectedPosition = -1
        shipAdapter.refreshShipList(shipList)

        if (viewModel.isShipListEmpty()) {
            rotateButton.visibility = View.GONE
            startButton.visibility = View.VISIBLE
            viewModel.startGameVisibility = true
        }
    }

    private fun initShipAdapter() {
        shipAdapter = ShipListAdapter(this)
        shipListView.adapter = shipAdapter

        shipListView.setOnItemClickListener { _, _, position, _ ->
            val selectedShip = shipAdapter.getItem(position) as Ship
            viewModel.selectedShip(selectedShip)
            shipAdapter.selectedPosition = position;
            shipAdapter.notifyDataSetChanged();
        }
    }

    private fun handleBoardClick(view: View, position: Int) {
        viewModel.handleBoardClick(view, position)
    }

    private fun setBlinkAnimation(view: View) {
        val animBlink: Animation =
            AnimationUtils.loadAnimation(this@SetupActivity, R.anim.blink_in);
        animBlink.setAnimationListener(this@SetupActivity)

        view.startAnimation(animBlink)

        scope.launch {
            delay(500)
            view.clearAnimation()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onAnimationRepeat(animation: Animation?) {

    }

    override fun onAnimationEnd(animation: Animation?) {

    }

    override fun onAnimationStart(animation: Animation?) {

    }

}
