package jp.techacademy.yusuke.shimozono.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

import kotlinx.android.synthetic.main.activity_favorite.*

class FavoriteActivity : AppCompatActivity() {
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private var mGenreRef: DatabaseReference? = null

    // ChildEventListenerについては、以下の公式リンク参照
    // https://firebase.google.com/docs/database/android/lists-of-data?hl=ja
    // DataSnapshotは１件ずつ渡ってくる。それをモデルクラスに詰め直して再描画
    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            Log.d("test99", "onChildAdded()開始")

            // ジャンルと質問IDを取得
            // favoriteListRefresh()でmGenreRefに格納されて受け渡された質問ID(=questionUid)をkeyとする
            var questionUid = dataSnapshot.key.toString()
            val map = dataSnapshot.value as Map<String, String>
            var genre = map["genre"] ?: ""
            Log.d("test99", "ジャンル：$genre 質問ID：$questionUid")

            // ジャンルと質問IDを元に、質問内容のスナップショットを取得
            // データベース全体(DatabaseReference)から、コンテンツ(ContentsPATH)→ジャンル(genre)→質問ID(questionUid)という順番で
            // 抽出したデータをquestionRefに格納する。(それがお気に入り一覧になる)
            var questionRef: DatabaseReference = mDatabaseReference.child(ContentsPATH).child(genre).child(questionUid)
            questionRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = snapshot.value as Map<String, String>
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

                    val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                        genre.toInt(), bytes, answerArrayList)
                    mQuestionArrayList.add(question)
                    mAdapter.notifyDataSetChanged()
                    Log.d("test99", "onChildAdded()完了")
                }
                override fun onCancelled(firebaseError: DatabaseError) {
                Log.d("test99", "onCancelled()")
                }
            })

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onChildRemoved(p0: DataSnapshot) {
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }

        override fun onCancelled(p0: DatabaseError) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        // タップしたドロワーメニューのタイトル(お気に入り一覧)をツールバーにも表示する
        title = getString(R.string.menu_favorite)

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged() // リストを再描画

        // listViewのitemタップ時に、詳細画面に遷移させる
        listView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面(QuestionDetailActivity)を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    // ①トップ画面のドロワーから遷移、②質問詳細画面から戻る、その2パターンでonResumeを実行する
    // その際に、favoriteListRefresh()を実行する
    override fun onResume() {
        super.onResume()
        favoriteListRefresh()
        Log.d("test99", "onResume")
    }

    // お気に入り一覧データを再取得し、リストに再描画させる
    private fun favoriteListRefresh() {
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        // mGenreRefに前回の履歴がある場合は、削除する
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener)
        }

        // Firebaseのデータベース全体(=DatabaseReference)から、お気に入り情報(=FavoritePATH)の中の、
        // 現在のログインユーザーID(=currentUser!!.uid)の下層に格納されている、questionID(=お気に入り登録されている質問ID)に
        // 合致する質問コンテンツのみをmGenreRefに格納する
        mGenreRef = mDatabaseReference.child(FavoritePATH).child(FirebaseAuth.getInstance().currentUser!!.uid)

        // ここでmEventListener(=ChildEventListener)を使って、現在のログインユーザーがお気に入り登録している質問のみをlistViewに表示する
        mGenreRef!!.addChildEventListener(mEventListener)
    }
}