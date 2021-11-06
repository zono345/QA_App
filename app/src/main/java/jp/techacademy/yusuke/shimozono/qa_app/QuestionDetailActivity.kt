package jp.techacademy.yusuke.shimozono.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private var favoriteState: Boolean = false // お気に入り状態を示す
    private var loginState: Boolean = false // ログイン状態を表す

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }
            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""
            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onCancelled(databaseError: DatabaseError) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        // ログイン状態の確認・更新＋お気に入りボタンの更新
        loginRefresh()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
            .child(mQuestion.questionUid).child(
            AnswersPATH
        )
        mAnswerRef.addChildEventListener(mEventListener)


        // お気に入りボタンを押した時の動作
        btnFav.setOnClickListener {
            val databaseReference = FirebaseDatabase.getInstance().reference
            // ユーザーIDとお気に入り登録状態を示すデータを格納するFirebaseのファイルパス(のようなもの)
            val favoriteRef = databaseReference.child(FavoritePATH)
                .child(FirebaseAuth.getInstance().currentUser!!.uid).child(mQuestion.questionUid)

            // イベントaddListenerForSingleValueEventは非同期で実行されるので、コードの記載順に実際の動作が合わない場合がある。
            // メソッドの動作順序が逆になる場合があるので、コードの書き方には要注意。
            // addListenerForSingleValueEventの完了後に実行させたいメソッドは、addListenerForSingleValueEvent内部の最後に書く必要がある。
            // addListenerForSingleValueEvent については以下リンクも参照のこと。
            // https://firebase.google.com/docs/database/android/read-and-write?hl=ja#kotlin+ktx_5
            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*, *>?
                    if (data == null) {
                        val data = HashMap<String, String>()
                        data["genre"] = mQuestion.genre.toString()

                        favoriteRef.setValue(data)
                    } else {
                        favoriteRef.removeValue()
                    }
                    favStateSearch()
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }


    // お気に入り登録状態に合わせて、お気に入りボタンの外観を変更するメソッド
    private fun btnAppearanceRefresh() {
        if (favoriteState) {
            btnFav.setBackgroundColor(getColor(R.color.btnColor_FavYes)) // ボタンの色を変更
            btnFav.text = getString(R.string.btn_text_favorite_yes) // ボタンのテキストを変更
            btnFav.setTextColor(getColor(R.color.black)) //テキストの色を変更
        } else {
            btnFav.setBackgroundColor(getColor(R.color.btnColor_FavNo)) // ボタンの色を変更
            btnFav.text = getString(R.string.btn_text_favorite_no) // ボタンのテキストを変更
            btnFav.setTextColor(getColor(R.color.gray)) //テキストの色を変更
        }
    }

    // お気に入り状態を検索し、favStateSearchResultにその結果を代入する
    private fun favStateSearch() {
        val databaseReference = FirebaseDatabase.getInstance().reference
        // ユーザーIDとお気に入り登録状態を示すデータを格納するFirebaseのファイルパス(のようなもの)
        // QaAppのデータベース全体 -> favorites -> ユーザーID -> 質問ID を参照している。
        // この参照部分がnullでない場合は、この質問IDがお気に入り登録されていることを意味する
        val favoriteRef = databaseReference.child(FavoritePATH)
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child(mQuestion.questionUid)

        // 以下は理解用のメモ
        // ①addListenerForSingleValueEvent()
        // 非同期で実行され、反復ごとに新しいインスタンスを作成するメソッド。
        // 一度だけデータを取得するのに使って、以後変更があっても利用しないようなケースでは、
        // addListenerForSingleValueEvent()にValueEventListenerを登録するのが便利。
        // ②DataSnapshot
        // DataSnapshotとは、あるURIのある瞬間のデータのスナップショット(=その瞬間Firebaseに存在するデータのこと)
        // DataSnapshot自体はデータの入れ物であり，データそのものを取り出すにはgetValue()メソッドを使う必要がある。
        // ③非同期の注意点
        // addListenerForSingleValueEventは非同期で実行されるので、favStateSearch()とbtnAppearanceRefresh()を
        // 別行に分けて書くと、実際のアプリ上の動作の順番が逆になってしまう場合がある。
        // なので、favStateSearch()の中にbtnAppearanceRefresh()を記載した。
        favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as Map<*, *>?
                favoriteState = data != null // dataがnullでない = お気に入り登録状態 を意味するのでその場合は、favoriteStateをtrueにする
                btnAppearanceRefresh()
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // ログイン状態を確認するメソッド。ログインしていた場合は、favStateSearch()を実行する
    private fun loginRefresh() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            loginState = false
            btnFav.visibility = View.GONE
        } else {
            loginState = true
            btnFav.visibility = View.VISIBLE
            favStateSearch()
        }
    }

    // 別画面から戻ってきた時にloginRefresh()を実行する
    override fun onResume() {
        super.onResume()
        loginRefresh()
    }
}




