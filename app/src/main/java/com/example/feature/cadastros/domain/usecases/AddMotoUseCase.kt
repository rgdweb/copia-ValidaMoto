package com.example.feature.cadastros.domain.usecases

import com.example.core.database.entity.Moto
import com.example.feature.cadastros.domain.repository.CadastrosRepository

class AddMotoUseCase(private val repository: CadastrosRepository) {
    suspend operator fun invoke(
        marca: String,
        modelo: String,
        ano: Int,
        placa: String,
        km: Int,
        status: String,
        foto: String
    ): Long {
        val moto = Moto(
            marca = marca,
            modelo = modelo,
            ano = ano,
            placa = placa,
            kmAtual = km,
            status = status,
            fotoCadastro = foto
        )
        return repository.insertMoto(moto)
    }
}
