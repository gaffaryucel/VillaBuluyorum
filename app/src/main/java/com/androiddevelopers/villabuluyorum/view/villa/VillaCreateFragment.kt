package com.androiddevelopers.villabuluyorum.view.villa

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.androiddevelopers.villabuluyorum.R
import com.androiddevelopers.villabuluyorum.adapter.ViewPagerAdapterForVillaCreate
import com.androiddevelopers.villabuluyorum.adapter.downloadImage
import com.androiddevelopers.villabuluyorum.databinding.FragmentVillaCreateBinding
import com.androiddevelopers.villabuluyorum.model.provinces.District
import com.androiddevelopers.villabuluyorum.model.provinces.Province
import com.androiddevelopers.villabuluyorum.model.villa.Facilities
import com.androiddevelopers.villabuluyorum.model.villa.Villa
import com.androiddevelopers.villabuluyorum.util.Status
import com.androiddevelopers.villabuluyorum.util.checkPermissionImageGallery
import com.androiddevelopers.villabuluyorum.util.hideBottomNavigation
import com.androiddevelopers.villabuluyorum.util.showBottomNavigation
import com.androiddevelopers.villabuluyorum.view.villa.VillaCreateFragmentDirections.Companion.actionGlobalNavigationProfile
import com.androiddevelopers.villabuluyorum.view.villa.VillaCreateFragmentDirections.Companion.actionVillaCreateFragmentToVillaCreateFacilitiesFragment
import com.androiddevelopers.villabuluyorum.viewmodel.villa.VillaCreateViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
@AndroidEntryPoint
class VillaCreateFragment : Fragment() {
    private val viewModel: VillaCreateViewModel by viewModels()
    private var _binding: FragmentVillaCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var facilitiesArg: Facilities

    private val provinceList = mutableListOf<Province>()
    private val districtList = mutableListOf<District>()

    private val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()

    private var viewPagerAdapter = ViewPagerAdapterForVillaCreate()
    private var selectedCoverImage: Uri? = null
    private lateinit var coverImageLauncher: ActivityResultLauncher<Intent>
    private val selectedOtherImages = mutableListOf<Uri>()
    private lateinit var otherImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVillaCreateBinding.inflate(inflater, container, false)
        setDropdownItems()
        setClickItems()
        viewModel.getAllProvince()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args: VillaCreateFragmentArgs by navArgs()
        args.facilities?.let {
            facilitiesArg = it
        } ?: run {
            facilitiesArg = Facilities()
        }

        with(binding) {
            setProgressBarVisibility = false
            setTextAddImageCoverVisibility = true
            setImageCoverVisibility = false
            setTextAddMoreImageVisibility = true
            setViewPagerVisibility = false

            //viewpager adapter ve indicatoru set ediyoruz
            viewPagerVillaCreate.adapter = viewPagerAdapter
            indicatorVillaCreate.setViewPager(viewPagerVillaCreate)
        }

        viewModel.setImageUriList(selectedOtherImages.toList())

        setupLaunchers()
        viewPagerAdapter.listenerImages = { images ->
            viewModel.setImageUriList(images.toList())
        }

    }

    private fun createVilla(id: String): Villa {
        val newVilla = Villa()
        with(binding) {
            with(newVilla) {
                villaId = id
                hostId = userId
                villaName = editTextTitleVillaCreate.text.toString()
                description = editTextDescriptionVillaCreate.text.toString()
                locationProvince = dropdownProvinceVillaCreate.text.toString()
                locationDistrict = dropdownDistrictVillaCreate.text.toString()
                locationNeighborhoodOrVillage =
                    dropdownNeighborhoodAndVillageVillaCreate.text.toString()
                locationAddress = editTextAddressVillaCreate.text.toString()
                nightlyRate = editTextNightlyRateVillaCreate.text.toString().toDoubleOrNull()
                capacity = dropdownCapacityVillaCreate.text.toString().toIntOrNull()
                bedroomCount = dropdownBedroomCountVillaCreate.toString().toIntOrNull()
                bedCount = dropdownBedCountVillaCreate.toString().toIntOrNull()
                bathroomCount = dropdownBathroomCountVillaCreate.toString().toIntOrNull()
                restroom = dropdownRestroomCountVillaCreate.toString().toIntOrNull()
                facilities = facilitiesArg
            }
        }

        return newVilla
    }

    private fun observeLiveData(owner: LifecycleOwner) {
        with(binding) {
            with(viewModel) {
                liveDataFirebaseStatus.observe(owner) {
                    when (it.status) {
                        Status.SUCCESS -> {
                            Navigation.findNavController(binding.root).navigate(
                                actionGlobalNavigationProfile()
                            )
                        }

                        Status.LOADING -> {
                            setProgressBarVisibility = it.data
                        }

                        Status.ERROR -> {

                        }
                    }

                }

                liveDataProvinceFromRoom.observe(owner) {
                    provinceList.clear()
                    provinceList.addAll(it.toList())

                    val listName = it.map { province -> province.name.toString() }

                    dropdownProvinceVillaCreate.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            android.R.id.text1,
                            listName.toList()
                        )
                    )
                }

                liveDataDistrictFromRoom.observe(owner) {

                    districtList.clear()
                    districtList.addAll(it.toList())

                    val listName = it.map { district -> district.name.toString() }

                    dropdownDistrictVillaCreate.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            android.R.id.text1,
                            listName.toList()
                        )
                    )

                }

                liveDataNeighborhoodFromRoom.observe(owner) { neighborhoods ->
                    val listName: MutableList<String> = mutableListOf()
                    listName.addAll(neighborhoods.map { neighborhood -> neighborhood.name.toString() })

                    liveDataVillageFromRoom.observe(owner) { villages ->
                        listName.addAll(villages.map { village -> village.name.toString() })

                        dropdownNeighborhoodAndVillageVillaCreate.setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                android.R.id.text1,
                                listName.toList()
                            )
                        )
                    }
                }

                imageUriList.observe(owner) { images ->
                    selectedOtherImages.clear()
                    selectedOtherImages.addAll(images.toList())
                    viewPagerAdapter.refreshList(images.toList())
                    with(binding) {
                        //indicatoru viewpager yeni liste ile set ediyoruz
                        indicatorVillaCreate.setViewPager(viewPagerVillaCreate)
                    }
                }

                imageSize.observe(owner) {
                    //seçilen resim olmadığında viewpager 'ı gizleyip boş bir resim gösteriyoruz
                    //resim seçildiğinde işlemi tersine alıyoruz
                    with(binding) {
                        if (it == 0 || it == null) {
                            setViewPagerVisibility = false
                        } else {
                            setViewPagerVisibility = true
                        }
                    }
                }
            }
        }

    }

    private fun setDropdownItems() {
        with(binding) {
            // string-array olarak belirleridğimiz numara listesini databinding ile görünüme gönderiyoruz
            numberAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                resources.getStringArray(R.array.numbers)
            )

            // seçtiğimiz illeri yakalıyoruz
            dropdownProvinceVillaCreate.setOnItemClickListener { parent, view, position, id ->
                // il değiştiğinde ilçe ve mahalle/köy içeriğini temizliyoruz
                dropdownDistrictVillaCreate.text = null
                dropdownNeighborhoodAndVillageVillaCreate.text = null

                //seçtiğimizz ilin id si ile ilçeleri getiriyoruz
                provinceList[position].id?.let { provinceId ->
                    viewModel.getAllDistrictById(
                        provinceId
                    )
                }
            }

            // seçtiğimiz ilçeleri yakalıyoruz
            dropdownDistrictVillaCreate.setOnItemClickListener { parent, view, position, id ->
                // ilçe değiştiğinde mahalle/köy içeriğini temizliyoruz
                dropdownNeighborhoodAndVillageVillaCreate.text = null

                // seçtiğimiz ilçenmin id si ile mahalle/köy leri getiriyoruz
                districtList[position].id?.let { districtId ->
                    viewModel.getAllNeighborhoodById(
                        districtId
                    )
                    viewModel.getAllVillageById(
                        districtId
                    )
                }
            }
        }
    }

    private fun setClickItems() {
        with(binding) {
            buttonSaveVillaCreate.setOnClickListener {
                val villaId = UUID.randomUUID().toString()
                viewModel.addImagesAndVillaToFirebase(
                    selectedCoverImage,
                    selectedOtherImages,
                    createVilla(villaId)
                )
            }

            textAddMoreFacility.setOnClickListener {
                //TODO: Create ekranında yapılan değişiklikeri kaybetmemek için giderken mevcut verileride gönder
                //TODO: Dönüşte bilgileri tekrar ekrana yazdır
                Navigation.findNavController(it)
                    .navigate(actionVillaCreateFragmentToVillaCreateFacilitiesFragment())
            }

            textAddImageCover.setOnClickListener {
                if (checkPermissionImageGallery(requireActivity(), 800)) {
                    openCoverImagePicker()
                }
            }

            buttonImageCoverEditVillaCreate.setOnClickListener {
                if (checkPermissionImageGallery(requireActivity(), 800)) {
                    openCoverImagePicker()
                }
            }

            textAddMoreImage.setOnClickListener {
                if (checkPermissionImageGallery(requireActivity(), 800)) {
                    openOtherImagePicker()
                }
            }
        }
    }

    private fun setupLaunchers() {
        coverImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { image ->
                        selectedCoverImage = image
                        selectedCoverImage?.let {
                            binding.setTextAddImageCoverVisibility = false
                            binding.setImageCoverVisibility = true
                            downloadImage(binding.imageCoverVillaCreate, image.toString())
                        }
                    }
                }
            }

        otherImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { image ->
                        selectedOtherImages.add(image)
                        viewModel.setImageUriList(selectedOtherImages.toList())
                    }
                }
            }
    }

    private fun openCoverImagePicker() {
        val imageIntent =
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        coverImageLauncher.launch(imageIntent)
    }

    private fun openOtherImagePicker() {
        val imageIntent =
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        otherImageLauncher.launch(imageIntent)
    }

    override fun onResume() {
        super.onResume()
        observeLiveData(viewLifecycleOwner)
        hideBottomNavigation(requireActivity())
    }

    override fun onPause() {
        super.onPause()
        showBottomNavigation(requireActivity())
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}