package fastcampus.aop.part3.chapter07

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// 뷰페이저에 사용하는 리사이클러뷰
class HouseViewPagerAdapter(val itemClicked: (HouseModel) -> Unit) :
    ListAdapter<HouseModel, HouseViewPagerAdapter.ItemViewHolder>(differ) {

    inner class ItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun bind(houseModel: HouseModel) {
            // 제목
            val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
            // 가격
            val priceTextView = view.findViewById<TextView>(R.id.priceTextView)
            // 이미지
            val thumbnailImageView = view.findViewById<ImageView>(R.id.thumbnailIamgeView)

            titleTextView.text = houseModel.title
            priceTextView.text = houseModel.price

            // 객체 클릭 했을 때 공유 하기 기능
            view.setOnClickListener {
                itemClicked(houseModel)
            }

            Glide
                .with(thumbnailImageView.context)
                .load(houseModel.imgUrl)
                .into(thumbnailImageView)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ItemViewHolder(
            inflater.inflate(
                R.layout.item_house_detail_for_viewpager,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {

        val differ = object : DiffUtil.ItemCallback<HouseModel>() {
            override fun areItemsTheSame(oldItem: HouseModel, newItem: HouseModel): Boolean {
                // 이전에 있던 아이템과 새로들어오는 아이템 값이 같은지 확인
                // id로 비교
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: HouseModel, newItem: HouseModel): Boolean {
                return oldItem == newItem
            }

        }
    }
}