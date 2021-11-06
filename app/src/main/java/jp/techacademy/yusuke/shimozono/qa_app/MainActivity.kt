package jp.techacademy.yusuke.shimozono.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
// findViewById()を呼び出さずに該当Viewを取得するために必要となるインポート宣言
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.util.Log

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mGenre = 0
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private var mGenreRef: DatabaseReference? = null


    // お気に入り一覧の課題対応のための変数 TODO 後でメンテナンスする
//    private lateinit var mFavoriteArrayList: ArrayList<Favorite>
//    private var mFavoriteRef: DatabaseReference? = null
//    private var mFavoriteQuestionRef: DatabaseReference? = null
//    private var favoriteState: Boolean = false


    private val mEventListener = object : ChildEventListener {

        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?

            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }


            // TODO favorite対応で修正中
            val question = Question(
                body, title, name, uid, dataSnapshot.key ?: "",
                mGenre, bytes, answerArrayList, /*favoriteArrayList*/
            )
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()



            // TODO オリジナルコード。一旦コメントアウトで保存しておく
//            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
//                mGenre, bytes, answerArrayList)
//            mQuestionArrayList.add(question)
//            mAdapter.notifyDataSetChanged()

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }

        override fun onCancelled(P0: DatabaseError) {
        }
    }


//    // TODO 課題用の追記　これやっぱいらない？？
//    // 「お気に入り一覧」のドロワーをタップした時の動作用のfavoriteListenerを作成しておく
//    private val favoriteListener = object : ChildEventListener {
//        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
//            val map = dataSnapshot.value as Map<String, String>
//            val uid = map["uid"] ?: ""
//            val questionUid = dataSnapshot.key as String
//            val genre = map["genre"].toString()
////            val favoriteQuestion = Favorite(genre, uid, questionUid)
////            mFavoriteArrayList.add(favoriteQuestion)
//        }
//
//        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
//        }
//
//        override fun onChildRemoved(p0: DataSnapshot) {
//        }
//
//        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
//        }
//
//        override fun onCancelled(P0: DatabaseError) {
//        }
//    }

//    // TODO 課題用の追記
//    // お気に入り質問を取得するリスナー
//    private val favoriteQuestionListener = object : ValueEventListener {
//        override fun onDataChange(dataSnapshot: DataSnapshot) {
//
//            // mEventListener の onChildAdded()の中身をコピペして、微修正。
//            val map = dataSnapshot.value as Map<String, String>
//            val title = map["title"] ?: ""
//            val body = map["body"] ?: ""
//            val name = map["name"] ?: ""
//            val uid = map["uid"] ?: ""
//            val imageString = map["image"] ?: ""
//            val bytes =
//                if (imageString.isNotEmpty()) {
//                    Base64.decode(imageString, Base64.DEFAULT)
//                } else {
//                    byteArrayOf()
//                }
//
//            val answerArrayList = ArrayList<Answer>()
//            val answerMap = map["answers"] as Map<String, String>?
//
//            if (answerMap != null) {
//                for (key in answerMap.keys) {
//                    val temp = answerMap[key] as Map<String, String>
//                    val answerBody = temp["body"] ?: ""
//                    val answerName = temp["name"] ?: ""
//                    val answerUid = temp["uid"] ?: ""
//                    val answer = Answer(answerBody, answerName, answerUid, key)
//                    answerArrayList.add(answer)
//                }
//            }
//
//            val favoriteQuestion = Question(
//                title, body, name, uid, dataSnapshot.key ?: "",
//                mGenre, bytes, answerArrayList)
//            mQuestionArrayList.add(favoriteQuestion)
//            mAdapter.notifyDataSetChanged()
//        }
//
//        override fun onCancelled(P0: DatabaseError) {
//        }
//    }










    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // idがtoolbarがインポート宣言により取得されているので
        // id名でActionBarのサポートを依頼
        setSupportActionBar(toolbar)

        // fabにClickリスナーを登録
        fab.setOnClickListener { view ->
            if (mGenre == 0) {
                Snackbar.make(view, getString(R.string.question_no_select_genre), Snackbar.LENGTH_LONG).show()
            } else {

            }
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        // ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference




















        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
//        mFavoriteArrayList = ArrayList<Favorite>() // TODO 課題用の追記

        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }






        // ログインチェック TODO　課題用の追記
        loginCheckAndFavoriteAppearance()


    }

    override fun onResume() {
        super.onResume()
        // 1:趣味を既定の選択とする
        if (mGenre == 0) {
            onNavigationItemSelected(nav_view.menu.getItem(0))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return  super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_hobby) {
            toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
        } else if (id == R.id.nav_life) {
            toolbar.title = getString(R.string.menu_life_label)
            mGenre = 2
        } else if (id == R.id.nav_health) {
            toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
        } else if (id == R.id.nav_computer) {
            toolbar.title = getString(R.string.menu_computer_label)
            mGenre = 4
        } else if (id == R.id.nav_favoriteList) { // お気に入り一覧 TODO 課題用の追記

            val intent = Intent(applicationContext, FavoriteActivity::class.java)
            startActivity(intent)

        }

        drawer_layout.closeDrawer(GravityCompat.START)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener)
        }
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
        mGenreRef!!.addChildEventListener(mEventListener)

        return true

    }







    // TODO 課題用の追記
    override fun onResumeFragments() {
        super.onResumeFragments()
        loginCheckAndFavoriteAppearance()
    }






    // ログインしている場合はお気に入り一覧をメニューに表示する TODO 課題用の追記
    private fun loginCheckAndFavoriteAppearance() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            findViewById<NavigationView>(R.id.nav_view).menu.findItem(R.id.nav_favoriteList).setVisible(true)
        } else {
        }
    }


}